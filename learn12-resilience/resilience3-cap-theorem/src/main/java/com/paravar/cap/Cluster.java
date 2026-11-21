package com.paravar.cap;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// three replicas in one JVM; the "network" is just a rule about which nodes can reach which
@Component
public class Cluster {

    public enum Mode { CP, AP }

    // Lamport clock + node name gives a total order, used for last-write-wins;
    // seen = clock of the version the writer already had, so we can tell stale from concurrent
    public record Versioned(String value, long clock, String node, long seen) {
        boolean newerThan(Versioned other) {
            return other == null || clock > other.clock || (clock == other.clock && node.compareTo(other.node) > 0);
        }
    }

    public record Settings(Mode mode, int w, int r, long lagMs, long hopMs) {
    }

    public static final List<String> NODES = List.of("n1", "n2", "n3");

    private final Map<String, Map<String, Versioned>> data = new ConcurrentHashMap<>();
    private final AtomicLong clock = new AtomicLong();
    private final ScheduledExecutorService lag = Executors.newSingleThreadScheduledExecutor();
    private volatile List<Set<String>> groups = List.of(Set.copyOf(NODES));
    private volatile Settings settings = new Settings(Mode.CP, 2, 2, 0, 0);

    public Cluster() {
        NODES.forEach(n -> data.put(n, new ConcurrentHashMap<>()));
    }

    public synchronized void configure(Settings s) {
        settings = s;
    }

    public Settings settings() {
        return settings;
    }

    public synchronized void partition(List<Set<String>> newGroups) {
        groups = List.copyOf(newGroups);
    }

    public boolean reachable(String from, String to) {
        return groups.stream().anyMatch(g -> g.contains(from) && g.contains(to));
    }

    public List<String> reachableFrom(String node) {
        return NODES.stream().filter(n -> reachable(node, n)).toList();
    }

    public record WriteResult(boolean accepted, int acks, String reason, Versioned version) {
    }

    public synchronized WriteResult write(String coordinator, String key, String value) {
        Settings s = settings;
        List<String> peers = reachableFrom(coordinator);
        long seen = Optional.ofNullable(data.get(coordinator).get(key)).map(Versioned::clock).orElse(0L);
        Versioned v = new Versioned(value, clock.incrementAndGet(), coordinator, seen);
        if (s.mode() == Mode.CP) {
            // CP: refuse unless W replicas can confirm; better unavailable than divergent
            if (peers.size() < s.w()) {
                return new WriteResult(false, 0, "only " + peers.size() + " of " + s.w() + " required replicas reachable", null);
            }
            hop(s);
            peers.forEach(n -> data.get(n).merge(key, v, Cluster::newest));
            return new WriteResult(true, peers.size(), "replicated synchronously", v);
        }
        // AP: always accept locally; peers get it later (lag) or at heal time
        data.get(coordinator).merge(key, v, Cluster::newest);
        List<String> others = peers.stream().filter(n -> !n.equals(coordinator)).toList();
        if (s.lagMs() == 0) {
            others.forEach(n -> data.get(n).merge(key, v, Cluster::newest));
        } else {
            lag.schedule(() -> others.forEach(n -> {
                if (reachable(coordinator, n)) {
                    data.get(n).merge(key, v, Cluster::newest);
                }
            }), s.lagMs(), TimeUnit.MILLISECONDS);
        }
        return new WriteResult(true, s.lagMs() == 0 ? peers.size() : 1, "accepted locally", v);
    }

    public record ReadResult(boolean ok, Versioned value, int replicasAsked, String reason) {
    }

    public ReadResult read(String coordinator, String key) {
        Settings s = settings;
        if (s.mode() == Mode.CP) {
            List<String> peers = reachableFrom(coordinator);
            if (peers.size() < s.r()) {
                return new ReadResult(false, null, peers.size(), "only " + peers.size() + " of " + s.r() + " required replicas reachable");
            }
            hop(s);
            // read R replicas, return the newest; with R + W > N one of them has the last write
            Versioned newest = peers.stream().limit(s.r()).map(n -> data.get(n).get(key))
                    .reduce(null, (a, b) -> b != null && b.newerThan(a) ? b : a);
            return new ReadResult(true, newest, s.r(), "quorum read");
        }
        return new ReadResult(true, data.get(coordinator).get(key), 1, "local read, may be stale");
    }

    public record HealReport(Map<String, Versioned> winners, List<Map<String, Object>> discarded) {
    }

    // anti-entropy on heal: every key converges to the newest version (last-write-wins)
    public synchronized HealReport heal() {
        groups = List.of(Set.copyOf(NODES));
        Map<String, Versioned> winners = new LinkedHashMap<>();
        List<Map<String, Object>> discarded = new ArrayList<>();
        data.values().stream().flatMap(m -> m.keySet().stream()).distinct().sorted().forEach(key -> {
            List<Versioned> versions = NODES.stream().map(n -> data.get(n).get(key)).filter(java.util.Objects::nonNull).distinct().toList();
            Versioned winner = versions.stream().max(Comparator.comparingLong(Versioned::clock).thenComparing(Versioned::node)).orElseThrow();
            winners.put(key, winner);
            // a version the winner had not seen was written concurrently: LWW throws it away
            versions.stream().filter(v -> !v.equals(winner) && v.clock() > winner.seen())
                    .forEach(lost -> discarded.add(Map.of("key", key, "lost", lost, "kept", winner)));
            NODES.forEach(n -> data.get(n).put(key, winner));
        });
        return new HealReport(winners, discarded);
    }

    public synchronized void reset() {
        data.values().forEach(Map::clear);
        groups = List.of(Set.copyOf(NODES));
        settings = new Settings(Mode.CP, 2, 2, 0, 0);
    }

    public Map<String, Object> view() {
        Map<String, Object> nodes = new LinkedHashMap<>();
        NODES.forEach(n -> nodes.put(n, new java.util.TreeMap<>(data.get(n))));
        Settings s = settings;
        return Map.of("settings", s, "partitions", groups, "nodes", nodes,
                "quorumOverlap", s.r() + s.w() > NODES.size()); // R + W > N: every read set meets every write set
    }

    public Optional<Versioned> local(String node, String key) {
        return Optional.ofNullable(data.get(node).get(key));
    }

    private static Versioned newest(Versioned a, Versioned b) {
        return b.newerThan(a) ? b : a;
    }

    private static void hop(Settings s) {
        if (s.hopMs() > 0) {
            try {
                Thread.sleep(s.hopMs()); // one network round trip to the replicas, in parallel
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

package com.paravar.mfa.totp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TotpService {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(5);

    // in-memory for the demo; a real app stores this row per user in its database
    static final class Enrollment {
        String encryptedSecret;
        String salt;
        long lastUsedStep = -1;
        int failures;
        Instant lockedUntil = Instant.EPOCH;
        final List<String> recoveryHashes = new ArrayList<>();
    }

    public enum Result { OK, INVALID, REPLAYED, LOCKED, NOT_ENROLLED }

    private final Map<String, Enrollment> enrollments = new ConcurrentHashMap<>();
    private final PasswordEncoder encoder;
    private final String encryptionKey;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TotpService(PasswordEncoder encoder, @Value("${mfa.encryption-key}") String encryptionKey, Clock clock) {
        this.encoder = encoder;
        this.encryptionKey = encryptionKey;
        this.clock = clock;
    }

    public boolean isEnrolled(String user) {
        return enrollments.containsKey(user);
    }

    // a code from the pending secret proves the user's app is set up before we save it
    public Optional<List<String>> enroll(String user, String pendingSecret, String code) {
        long now = Totp.step(clock.instant().getEpochSecond());
        if (!matchesWindow(pendingSecret, code, now)) {
            return Optional.empty();
        }
        Enrollment e = new Enrollment();
        e.salt = KeyGenerators.string().generateKey();
        e.encryptedSecret = cipher(e.salt).encrypt(pendingSecret);
        e.lastUsedStep = now;
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String rc = "%04d-%04d".formatted(random.nextInt(10_000), random.nextInt(10_000));
            codes.add(rc);
            e.recoveryHashes.add(encoder.encode(rc));
        }
        enrollments.put(user, e);
        return Optional.of(codes);
    }

    public synchronized Result verify(String user, String input) {
        Enrollment e = enrollments.get(user);
        if (e == null) {
            return Result.NOT_ENROLLED;
        }
        if (clock.instant().isBefore(e.lockedUntil)) {
            return Result.LOCKED;
        }
        Result result = input.contains("-") ? useRecoveryCode(e, input) : checkCode(e, input);
        if (result == Result.OK) {
            e.failures = 0;
        } else if (++e.failures >= MAX_FAILURES) {
            // 6 digits = 1,000,000 values; a lockout stops brute force guessing
            e.lockedUntil = clock.instant().plus(LOCK_TIME);
            e.failures = 0;
        }
        return result;
    }

    public void disable(String user) {
        enrollments.remove(user);
    }

    public int recoveryCodesLeft(String user) {
        Enrollment e = enrollments.get(user);
        return e == null ? 0 : e.recoveryHashes.size();
    }

    private Result checkCode(Enrollment e, String code) {
        String secret = cipher(e.salt).decrypt(e.encryptedSecret);
        long now = Totp.step(clock.instant().getEpochSecond());
        for (long step = now - 1; step <= now + 1; step++) { // +-30 s for clock drift
            if (Totp.code(secret, step).equals(code)) {
                if (step <= e.lastUsedStep) {
                    return Result.REPLAYED; // each code works once, even inside its window
                }
                e.lastUsedStep = step;
                return Result.OK;
            }
        }
        return Result.INVALID;
    }

    private Result useRecoveryCode(Enrollment e, String code) {
        for (Iterator<String> it = e.recoveryHashes.iterator(); it.hasNext(); ) {
            if (encoder.matches(code, it.next())) {
                it.remove(); // single use
                return Result.OK;
            }
        }
        return Result.INVALID;
    }

    private boolean matchesWindow(String secret, String code, long now) {
        for (long step = now - 1; step <= now + 1; step++) {
            if (Totp.code(secret, step).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private TextEncryptor cipher(String salt) {
        // AES-256-GCM; the key comes from config so a DB leak alone does not expose secrets
        return Encryptors.delux(encryptionKey, salt);
    }
}

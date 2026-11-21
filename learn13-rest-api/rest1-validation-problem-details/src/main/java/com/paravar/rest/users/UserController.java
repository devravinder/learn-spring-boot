package com.paravar.rest.users;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/users")
public class UserController {

    public record User(long id, String name, String email, int age, String city) {
    }

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    // @Validated(group) instead of @Valid picks which constraints apply to this operation
    @PostMapping
    public ResponseEntity<User> create(@Validated(UserRequest.OnCreate.class) @RequestBody UserRequest request) {
        if (users.values().stream().anyMatch(u -> u.email().equalsIgnoreCase(request.email()))) {
            throw new DuplicateEmailException(request.email());
        }
        User user = new User(ids.incrementAndGet(), request.name(), request.email(), request.age(), request.address().city());
        users.put(user.id(), user);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(user.id())).body(user);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable long id, @Validated(UserRequest.OnUpdate.class) @RequestBody UserRequest request) {
        if (!users.containsKey(id)) {
            throw new UserNotFoundException(id);
        }
        User user = new User(id, request.name(), request.email(), request.age(), request.address().city());
        users.put(id, user);
        return user;
    }

    // constraints directly on parameters: Spring MVC runs method validation itself
    @GetMapping("/{id}")
    public User get(@PathVariable @Min(1) long id) {
        User user = users.get(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }

    @GetMapping
    public List<User> list(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return users.values().stream().limit(limit).toList();
    }

    @GetMapping("/boom")
    public User boom() {
        throw new IllegalStateException("database password is hunter2"); // must never reach the client
    }
}

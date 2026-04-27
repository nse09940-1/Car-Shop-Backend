package main.domain.user;

import java.util.Objects;
import java.util.UUID;

public record User(
        UUID id,
        String name,
        String email,
        Role role) {

    public User {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(role, "role is required");
    }
}


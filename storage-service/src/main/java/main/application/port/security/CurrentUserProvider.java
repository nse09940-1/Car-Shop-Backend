package main.application.port.security;

import java.util.UUID;

public interface CurrentUserProvider {
    UUID currentUserId();

    boolean hasRole(String role);

    boolean hasAnyRole(String... roles);
}

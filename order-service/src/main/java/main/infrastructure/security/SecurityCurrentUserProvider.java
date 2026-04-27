package main.infrastructure.security;

import main.application.port.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {
    private static final String USER_ID_CLAIM = "app_user_id";

    @Override
    public UUID currentUserId() {
        Authentication authentication = getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new AccessDeniedException("JWT authentication is required");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String rawUserId = jwt.getClaimAsString(USER_ID_CLAIM);
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new AccessDeniedException("JWT claim app_user_id is required");
        }

        try {
            return UUID.fromString(rawUserId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("JWT claim app_user_id must be a UUID");
        }
    }

    @Override
    public boolean hasRole(String role) {
        return getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(toAuthority(role)));
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return authentication;
    }

    private String toAuthority(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}

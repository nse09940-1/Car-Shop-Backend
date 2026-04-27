package main;

import main.infrastructure.security.KeycloakRealmRoleConverter;
import main.infrastructure.security.SecurityCurrentUserProvider;
import main.infrastructure.security.SecurityRoles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityComponentsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keycloakRoleConverter_mapsRealmRolesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("WAREHOUSE_ADMIN", "ADMIN")))
                .build();

        var authorities = new KeycloakRealmRoleConverter().convert(jwt);

        assertNotNull(authorities);
        assertEquals(Set.of("ROLE_WAREHOUSE_ADMIN", "ROLE_ADMIN"),
                authorities.stream().map(authority -> authority.getAuthority()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void currentUserProvider_readsUserIdAndRolesFromSecurityContext() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("app_user_id", userId.toString())
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_WAREHOUSE_ADMIN"), new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityCurrentUserProvider provider = new SecurityCurrentUserProvider();

        assertEquals(userId, provider.currentUserId());
        assertTrue(provider.hasRole(SecurityRoles.WAREHOUSE_ADMIN));
        assertTrue(provider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.USER));
        assertFalse(provider.hasRole(SecurityRoles.USER));
    }
}

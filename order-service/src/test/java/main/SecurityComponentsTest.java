package main;

import main.application.port.security.CurrentUserProvider;
import main.domain.configuration.CarConfiguration;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.infrastructure.repository.InMemoryCustomOrderRepository;
import main.infrastructure.repository.InMemoryStockOrderRepository;
import main.infrastructure.security.KeycloakRealmRoleConverter;
import main.infrastructure.security.OrderAccessEvaluator;
import main.infrastructure.security.SecurityCurrentUserProvider;
import main.infrastructure.security.SecurityRoles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
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
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .build();

        var authorities = new KeycloakRealmRoleConverter().convert(jwt);

        assertNotNull(authorities);
        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"),
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
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_MANAGER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityCurrentUserProvider provider = new SecurityCurrentUserProvider();

        assertEquals(userId, provider.currentUserId());
        assertTrue(provider.hasRole(SecurityRoles.USER));
        assertTrue(provider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.MANAGER));
        assertFalse(provider.hasRole(SecurityRoles.ADMIN));
    }

    @Test
    void orderAccessEvaluator_honorsOwnerManagerAndAdminRules() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID stockOrderId = UUID.randomUUID();
        UUID customOrderId = UUID.randomUUID();

        InMemoryStockOrderRepository stockOrderRepository = new InMemoryStockOrderRepository();
        stockOrderRepository.save(new StockCarOrder(
                stockOrderId,
                ownerId,
                UUID.randomUUID(),
                LocalDateTime.now(),
                UUID.randomUUID(),
                StockOrderStatus.CREATED));

        InMemoryCustomOrderRepository customOrderRepository = new InMemoryCustomOrderRepository();
        customOrderRepository.save(new CustomCarOrder(
                customOrderId,
                ownerId,
                UUID.randomUUID(),
                LocalDateTime.now(),
                UUID.randomUUID(),
                new CarConfiguration(null, null, null, null),
                new main.domain.Money(3_000_000),
                CustomOrderStatus.CREATED));

        OrderAccessEvaluator userEvaluator = new OrderAccessEvaluator(
                stockOrderRepository,
                customOrderRepository,
                new StubCurrentUserProvider(ownerId, Set.of(SecurityRoles.USER)));
        assertTrue(userEvaluator.canAccessStockOrder(stockOrderId));
        assertTrue(userEvaluator.canChangeStockStatus(stockOrderId, StockOrderStatus.PAID));
        assertTrue(userEvaluator.canChangeCustomStatus(customOrderId, CustomOrderStatus.PAID));
        assertTrue(userEvaluator.canChangeCustomStatus(customOrderId, CustomOrderStatus.CANCELLED));

        OrderAccessEvaluator otherUserEvaluator = new OrderAccessEvaluator(
                stockOrderRepository,
                customOrderRepository,
                new StubCurrentUserProvider(otherUserId, Set.of(SecurityRoles.USER)));
        assertFalse(otherUserEvaluator.canAccessStockOrder(stockOrderId));
        assertFalse(otherUserEvaluator.canChangeStockStatus(stockOrderId, StockOrderStatus.PAID));

        OrderAccessEvaluator managerEvaluator = new OrderAccessEvaluator(
                stockOrderRepository,
                customOrderRepository,
                new StubCurrentUserProvider(UUID.randomUUID(), Set.of(SecurityRoles.MANAGER)));
        assertTrue(managerEvaluator.canAccessStockOrder(stockOrderId));
        assertTrue(managerEvaluator.canChangeStockStatus(stockOrderId, StockOrderStatus.MANAGER_APPROVED));
        assertTrue(managerEvaluator.canChangeStockStatus(stockOrderId, StockOrderStatus.READY_FOR_HANDOVER));
        assertTrue(managerEvaluator.canChangeCustomStatus(customOrderId, CustomOrderStatus.AWAITING_PAYMENT));
        assertTrue(managerEvaluator.canChangeCustomStatus(customOrderId, CustomOrderStatus.READY_FOR_HANDOVER));

        OrderAccessEvaluator warehouseEvaluator = new OrderAccessEvaluator(
                stockOrderRepository,
                customOrderRepository,
                new StubCurrentUserProvider(UUID.randomUUID(), Set.of(SecurityRoles.WAREHOUSE_ADMIN)));
        assertFalse(warehouseEvaluator.canChangeCustomStatus(customOrderId, CustomOrderStatus.PAID));
    }

    private record StubCurrentUserProvider(UUID currentUserId, Set<String> roles) implements CurrentUserProvider {
        @Override
        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        @Override
        public boolean hasAnyRole(String... roles) {
            for (String role : roles) {
                if (hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
    }
}

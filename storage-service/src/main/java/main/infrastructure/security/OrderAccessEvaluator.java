package main.infrastructure.security;

import main.application.port.repository.CustomOrderRepository;
import main.application.port.repository.StockOrderRepository;
import main.application.port.security.CurrentUserProvider;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockOrderStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderAccess")
public class OrderAccessEvaluator {
    private final StockOrderRepository stockOrderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final CurrentUserProvider currentUserProvider;

    public OrderAccessEvaluator(
            StockOrderRepository stockOrderRepository,
            CustomOrderRepository customOrderRepository,
            CurrentUserProvider currentUserProvider) {
        this.stockOrderRepository = stockOrderRepository;
        this.customOrderRepository = customOrderRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public boolean canAccessStockOrder(UUID orderId) {
        if (currentUserProvider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.MANAGER)) {
            return true;
        }
        if (!currentUserProvider.hasRole(SecurityRoles.USER)) {
            return false;
        }
        return stockOrderRepository.isOwner(orderId, currentUserProvider.currentUserId());
    }

    public boolean canChangeStockStatus(UUID orderId, StockOrderStatus newStatus) {
        if (currentUserProvider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.MANAGER)) {
            return true;
        }
        if (newStatus != StockOrderStatus.CANCELLED || !currentUserProvider.hasRole(SecurityRoles.USER)) {
            return false;
        }
        return stockOrderRepository.isOwner(orderId, currentUserProvider.currentUserId());
    }

    public boolean canAccessCustomOrder(UUID orderId) {
        if (currentUserProvider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.MANAGER)) {
            return true;
        }
        if (!currentUserProvider.hasRole(SecurityRoles.USER)) {
            return false;
        }
        return customOrderRepository.isOwner(orderId, currentUserProvider.currentUserId());
    }

    public boolean canChangeCustomStatus(UUID orderId, CustomOrderStatus newStatus) {
        if (currentUserProvider.hasRole(SecurityRoles.ADMIN)) {
            return true;
        }
        if (newStatus == CustomOrderStatus.WAREHOUSE_APPROVED) {
            return currentUserProvider.hasRole(SecurityRoles.WAREHOUSE_ADMIN);
        }
        if (newStatus == CustomOrderStatus.CANCELLED) {
            if (currentUserProvider.hasRole(SecurityRoles.MANAGER)) {
                return true;
            }
            return currentUserProvider.hasRole(SecurityRoles.USER)
                    && customOrderRepository.isOwner(orderId, currentUserProvider.currentUserId());
        }
        return currentUserProvider.hasRole(SecurityRoles.MANAGER);
    }
}

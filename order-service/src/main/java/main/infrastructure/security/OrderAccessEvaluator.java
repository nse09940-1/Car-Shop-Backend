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
        if (currentUserProvider.hasRole(SecurityRoles.ADMIN)) {
            return true;
        }
        if (currentUserProvider.hasRole(SecurityRoles.MANAGER)) {
            return newStatus == StockOrderStatus.MANAGER_APPROVED
                    || newStatus == StockOrderStatus.AWAITING_PAYMENT
                    || newStatus == StockOrderStatus.READY_FOR_HANDOVER
                    || newStatus == StockOrderStatus.CANCELLED
                    || newStatus == StockOrderStatus.COMPLETED;
        }
        if (!currentUserProvider.hasRole(SecurityRoles.USER)) {
            return false;
        }
        if (newStatus != StockOrderStatus.CANCELLED && newStatus != StockOrderStatus.PAID) {
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
        if (currentUserProvider.hasRole(SecurityRoles.MANAGER)) {
            return newStatus == CustomOrderStatus.AWAITING_PAYMENT
                    || newStatus == CustomOrderStatus.READY_FOR_HANDOVER
                    || (newStatus == CustomOrderStatus.CANCELLED && canCancelCustomOrder(orderId))
                    || newStatus == CustomOrderStatus.COMPLETED;
        }
        if (!currentUserProvider.hasRole(SecurityRoles.USER)) {
            return false;
        }
        if (newStatus == CustomOrderStatus.CANCELLED) {
            return canCancelCustomOrder(orderId)
                    && customOrderRepository.isOwner(orderId, currentUserProvider.currentUserId());
        }
        if (newStatus != CustomOrderStatus.PAID) {
            return false;
        }
        return customOrderRepository.isOwner(orderId, currentUserProvider.currentUserId());
    }

    private boolean canCancelCustomOrder(UUID orderId) {
        return customOrderRepository.findById(orderId)
                .filter(order -> order.status() == CustomOrderStatus.CREATED
                        || order.status() == CustomOrderStatus.WAREHOUSE_APPROVED
                        || order.status() == CustomOrderStatus.AWAITING_PAYMENT)
                .isPresent();
    }
}

package main.infrastructure.policy;

import main.application.port.policy.OrderTransitionPolicy;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockOrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultOrderTransitionPolicy implements OrderTransitionPolicy {
    private final Map<StockOrderStatus, Set<StockOrderStatus>> stockTransitions;
    private final Map<CustomOrderStatus, Set<CustomOrderStatus>> customTransitions;

    public DefaultOrderTransitionPolicy() {
        this.stockTransitions = buildStockTransitions();
        this.customTransitions = buildCustomTransitions();
    }

    @Override
    public boolean canTransition(StockOrderStatus from, StockOrderStatus to) {
        return stockTransitions.getOrDefault(from, Set.of()).contains(to);
    }

    @Override
    public boolean canTransition(CustomOrderStatus from, CustomOrderStatus to) {
        return customTransitions.getOrDefault(from, Set.of()).contains(to);
    }

    private static Map<StockOrderStatus, Set<StockOrderStatus>> buildStockTransitions() {
        Map<StockOrderStatus, Set<StockOrderStatus>> map = new EnumMap<>(StockOrderStatus.class);
        map.put(StockOrderStatus.CREATED, EnumSet.of(StockOrderStatus.MANAGER_APPROVED, StockOrderStatus.CANCELLED));
        map.put(StockOrderStatus.MANAGER_APPROVED, EnumSet.of(StockOrderStatus.AWAITING_PAYMENT, StockOrderStatus.CANCELLED));
        map.put(StockOrderStatus.AWAITING_PAYMENT, EnumSet.of(StockOrderStatus.PAID, StockOrderStatus.CANCELLED));
        map.put(StockOrderStatus.PAID, EnumSet.of(StockOrderStatus.AWAITING_DELIVERY, StockOrderStatus.CANCELLED));
        map.put(StockOrderStatus.AWAITING_DELIVERY, EnumSet.of(StockOrderStatus.READY_FOR_HANDOVER));
        map.put(StockOrderStatus.READY_FOR_HANDOVER, EnumSet.of(StockOrderStatus.COMPLETED));
        map.put(StockOrderStatus.COMPLETED, EnumSet.noneOf(StockOrderStatus.class));
        map.put(StockOrderStatus.CANCELLED, EnumSet.noneOf(StockOrderStatus.class));
        return map;
    }

    private static Map<CustomOrderStatus, Set<CustomOrderStatus>> buildCustomTransitions() {
        Map<CustomOrderStatus, Set<CustomOrderStatus>> map = new EnumMap<>(CustomOrderStatus.class);
        map.put(CustomOrderStatus.CREATED, EnumSet.of(CustomOrderStatus.WAREHOUSE_APPROVED, CustomOrderStatus.CANCELLED));
        map.put(CustomOrderStatus.WAREHOUSE_APPROVED, EnumSet.of(CustomOrderStatus.AWAITING_PAYMENT, CustomOrderStatus.CANCELLED));
        map.put(CustomOrderStatus.AWAITING_PAYMENT, EnumSet.of(CustomOrderStatus.PAID, CustomOrderStatus.CANCELLED));
        map.put(CustomOrderStatus.PAID, EnumSet.of(CustomOrderStatus.AWAITING_DELIVERY));
        map.put(CustomOrderStatus.AWAITING_DELIVERY, EnumSet.of(CustomOrderStatus.READY_FOR_HANDOVER));
        map.put(CustomOrderStatus.READY_FOR_HANDOVER, EnumSet.of(CustomOrderStatus.COMPLETED));
        map.put(CustomOrderStatus.COMPLETED, EnumSet.noneOf(CustomOrderStatus.class));
        map.put(CustomOrderStatus.CANCELLED, EnumSet.noneOf(CustomOrderStatus.class));
        return map;
    }
}

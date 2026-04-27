package main.application.port.policy;

import main.domain.order.CustomOrderStatus;
import main.domain.order.StockOrderStatus;

public interface OrderTransitionPolicy {
    boolean canTransition(StockOrderStatus from, StockOrderStatus to);

    boolean canTransition(CustomOrderStatus from, CustomOrderStatus to);
}


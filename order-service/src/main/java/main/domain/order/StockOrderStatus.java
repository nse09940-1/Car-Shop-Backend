package main.domain.order;

public enum StockOrderStatus {
    CREATED,
    MANAGER_APPROVED,
    AWAITING_PAYMENT,
    PAID,
    AWAITING_DELIVERY,
    READY_FOR_HANDOVER,
    COMPLETED,
    CANCELLED
}


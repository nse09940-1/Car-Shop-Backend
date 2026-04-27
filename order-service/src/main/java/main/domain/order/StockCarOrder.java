package main.domain.order;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class StockCarOrder extends Order {
    private final UUID carId;
    private StockOrderStatus status;

    public StockCarOrder(
            UUID id,
            UUID customerId,
            UUID managerId,
            LocalDateTime createdAt,
            UUID carId,
            StockOrderStatus status) {
        super(id, customerId, managerId, createdAt);
        this.carId = Objects.requireNonNull(carId, "carId is required");
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public UUID carId() {
        return carId;
    }

    public StockOrderStatus status() {
        return status;
    }

    public void setStatus(StockOrderStatus status) {
        this.status = Objects.requireNonNull(status, "status is required");
    }
}


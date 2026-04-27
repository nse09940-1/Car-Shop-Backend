package main.domain.order;

import main.domain.Money;
import main.domain.configuration.CarConfiguration;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class CustomCarOrder extends Order {
    private final UUID carModelId;
    private final CarConfiguration configuration;
    private final Money totalPrice;
    private CustomOrderStatus status;

    public CustomCarOrder(
            UUID id,
            UUID customerId,
            UUID managerId,
            LocalDateTime createdAt,
            UUID carModelId,
            CarConfiguration configuration,
            Money totalPrice,
            CustomOrderStatus status) {
        super(id, customerId, managerId, createdAt);
        this.carModelId = Objects.requireNonNull(carModelId, "carModelId is required");
        this.configuration = Objects.requireNonNull(configuration, "configuration is required");
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice is required");
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public UUID carModelId() {
        return carModelId;
    }

    public CarConfiguration configuration() {
        return configuration;
    }

    public Money totalPrice() {
        return totalPrice;
    }

    public CustomOrderStatus status() {
        return status;
    }

    public void setStatus(CustomOrderStatus status) {
        this.status = Objects.requireNonNull(status, "status is required");
    }
}


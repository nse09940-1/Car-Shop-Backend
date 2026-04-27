package main.domain.order;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public abstract class Order {
    private final UUID id;
    private final UUID customerId;
    private final UUID managerId;
    private final LocalDateTime createdTime;

    protected Order(UUID id, UUID customerId, UUID managerId, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.managerId = Objects.requireNonNull(managerId, "managerId is required");
        this.createdTime = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID managerId() {
        return managerId;
    }

    public LocalDateTime createdAt() {
        return createdTime;
    }
}


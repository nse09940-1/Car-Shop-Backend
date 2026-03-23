package main.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_drive_requests")
public class TestDriveJpaEntity extends BaseJpaEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "car_id", nullable = false)
    private UUID carId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}


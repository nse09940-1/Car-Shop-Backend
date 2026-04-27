package main.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import main.domain.order.StockOrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_orders")
public class StockOrderJpaEntity extends BaseJpaEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Column(name = "car_id", nullable = false)
    private UUID carId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockOrderStatus status;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public StockOrderStatus getStatus() {
        return status;
    }

    public void setStatus(StockOrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}


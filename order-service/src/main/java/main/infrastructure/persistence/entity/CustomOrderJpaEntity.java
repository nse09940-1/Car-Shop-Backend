package main.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import main.domain.order.CustomOrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "custom_orders")
public class CustomOrderJpaEntity extends BaseJpaEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Column(name = "car_model_id", nullable = false)
    private UUID carModelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomOrderStatus status;

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "wheel_option_id")
    private UUID wheelOptionId;

    @Column(name = "transmission_option_id")
    private UUID transmissionOptionId;

    @Column(name = "steering_option_id")
    private UUID steeringOptionId;

    @Column(name = "interior_option_id")
    private UUID interiorOptionId;

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

    public UUID getCarModelId() {
        return carModelId;
    }

    public void setCarModelId(UUID carModelId) {
        this.carModelId = carModelId;
    }

    public CustomOrderStatus getStatus() {
        return status;
    }

    public void setStatus(CustomOrderStatus status) {
        this.status = status;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public UUID getWheelOptionId() {
        return wheelOptionId;
    }

    public void setWheelOptionId(UUID wheelOptionId) {
        this.wheelOptionId = wheelOptionId;
    }

    public UUID getTransmissionOptionId() {
        return transmissionOptionId;
    }

    public void setTransmissionOptionId(UUID transmissionOptionId) {
        this.transmissionOptionId = transmissionOptionId;
    }

    public UUID getSteeringOptionId() {
        return steeringOptionId;
    }

    public void setSteeringOptionId(UUID steeringOptionId) {
        this.steeringOptionId = steeringOptionId;
    }

    public UUID getInteriorOptionId() {
        return interiorOptionId;
    }

    public void setInteriorOptionId(UUID interiorOptionId) {
        this.interiorOptionId = interiorOptionId;
    }
}


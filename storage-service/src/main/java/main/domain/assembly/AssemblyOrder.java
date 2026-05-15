package main.domain.assembly;

import contracts.events.OrderType;

import java.time.Instant;
import java.util.UUID;

public class AssemblyOrder {
    private UUID id;
    private UUID sourceOrderId;
    private OrderType sourceOrderType;
    private UUID carId;
    private UUID carModelId;
    private UUID warehouseEmployeeId;
    private AssemblyOrderStatus status;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSourceOrderId() {
        return sourceOrderId;
    }

    public void setSourceOrderId(UUID sourceOrderId) {
        this.sourceOrderId = sourceOrderId;
    }

    public OrderType getSourceOrderType() {
        return sourceOrderType;
    }

    public void setSourceOrderType(OrderType sourceOrderType) {
        this.sourceOrderType = sourceOrderType;
    }

    public UUID getCarId() {
        return carId;
    }

    public void setCarId(UUID carId) {
        this.carId = carId;
    }

    public UUID getCarModelId() {
        return carModelId;
    }

    public void setCarModelId(UUID carModelId) {
        this.carModelId = carModelId;
    }

    public UUID getWarehouseEmployeeId() {
        return warehouseEmployeeId;
    }

    public void setWarehouseEmployeeId(UUID warehouseEmployeeId) {
        this.warehouseEmployeeId = warehouseEmployeeId;
    }

    public AssemblyOrderStatus getStatus() {
        return status;
    }

    public void setStatus(AssemblyOrderStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

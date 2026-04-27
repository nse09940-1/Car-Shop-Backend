package main.infrastructure.persistence.entity;

import contracts.events.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import main.domain.assembly.AssemblyOrderStatus;

import java.util.UUID;

@Entity
@Table(name = "assembly_orders")
public class AssemblyOrderJpaEntity extends BaseJpaEntity {

    @Column(name = "source_order_id", nullable = false)
    private UUID sourceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_order_type", nullable = false)
    private OrderType sourceOrderType;

    @Column(name = "car_id")
    private UUID carId;

    @Column(name = "car_model_id")
    private UUID carModelId;

    @Column(name = "warehouse_employee_id")
    private UUID warehouseEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssemblyOrderStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

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
}

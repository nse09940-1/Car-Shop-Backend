package main.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "assembly_order_required_parts")
@IdClass(AssemblyOrderRequiredPartId.class)
public class AssemblyOrderRequiredPartJpaEntity {
    @Id
    @Column(name = "assembly_order_id", nullable = false)
    private UUID assemblyOrderId;

    @Id
    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public UUID getAssemblyOrderId() {
        return assemblyOrderId;
    }

    public void setAssemblyOrderId(UUID assemblyOrderId) {
        this.assemblyOrderId = assemblyOrderId;
    }

    public UUID getPartId() {
        return partId;
    }

    public void setPartId(UUID partId) {
        this.partId = partId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

package main.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class AssemblyOrderRequiredPartId implements Serializable {
    private UUID assemblyOrderId;
    private UUID partId;

    public AssemblyOrderRequiredPartId() {
    }

    public AssemblyOrderRequiredPartId(UUID assemblyOrderId, UUID partId) {
        this.assemblyOrderId = assemblyOrderId;
        this.partId = partId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssemblyOrderRequiredPartId that)) {
            return false;
        }
        return Objects.equals(assemblyOrderId, that.assemblyOrderId) && Objects.equals(partId, that.partId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assemblyOrderId, partId);
    }
}

package main.infrastructure.persistence.adapter;

import main.application.port.repository.AssemblyOrderRepository;
import main.domain.assembly.AssemblyOrder;
import main.domain.exception.EntityNotFoundException;
import main.infrastructure.persistence.entity.AssemblyOrderJpaEntity;
import main.infrastructure.persistence.repository.AssemblyOrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAssemblyOrderRepositoryAdapter implements AssemblyOrderRepository {
    private final AssemblyOrderJpaRepository jpaRepository;

    public JpaAssemblyOrderRepositoryAdapter(AssemblyOrderJpaRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "jpaRepository is required");
    }

    @Override
    public AssemblyOrder save(AssemblyOrder order) {
        return toDomain(jpaRepository.save(toEntity(order)));
    }

    @Override
    public Optional<AssemblyOrder> findById(UUID id) {
        return jpaRepository.findByIdAndRemovedFalse(id).map(this::toDomain);
    }

    @Override
    public List<AssemblyOrder> findAll() {
        return jpaRepository.findAllByRemovedFalse().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AssemblyOrder> findBySourceOrderId(UUID sourceOrderId) {
        return jpaRepository.findBySourceOrderIdAndRemovedFalse(sourceOrderId).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        AssemblyOrderJpaEntity entity = jpaRepository.findByIdAndRemovedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found"));
        entity.setRemoved(true);
        jpaRepository.save(entity);
    }

    private AssemblyOrderJpaEntity toEntity(AssemblyOrder order) {
        AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
        entity.setId(order.getId());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        entity.setRemoved(false);
        entity.setSourceOrderId(order.getSourceOrderId());
        entity.setSourceOrderType(order.getSourceOrderType());
        entity.setCarId(order.getCarId());
        entity.setCarModelId(order.getCarModelId());
        entity.setWarehouseEmployeeId(order.getWarehouseEmployeeId());
        entity.setStatus(order.getStatus());
        entity.setFailureReason(order.getFailureReason());
        return entity;
    }

    private AssemblyOrder toDomain(AssemblyOrderJpaEntity entity) {
        AssemblyOrder order = new AssemblyOrder();
        order.setId(entity.getId());
        order.setCreatedAt(entity.getCreatedAt());
        order.setUpdatedAt(entity.getUpdatedAt());
        order.setSourceOrderId(entity.getSourceOrderId());
        order.setSourceOrderType(entity.getSourceOrderType());
        order.setCarId(entity.getCarId());
        order.setCarModelId(entity.getCarModelId());
        order.setWarehouseEmployeeId(entity.getWarehouseEmployeeId());
        order.setStatus(entity.getStatus());
        order.setFailureReason(entity.getFailureReason());
        return order;
    }
}

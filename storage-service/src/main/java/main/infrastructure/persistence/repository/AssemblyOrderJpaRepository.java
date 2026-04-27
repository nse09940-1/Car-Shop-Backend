package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.AssemblyOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssemblyOrderJpaRepository extends JpaRepository<AssemblyOrderJpaEntity, UUID> {
    Optional<AssemblyOrderJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<AssemblyOrderJpaEntity> findAllByRemovedFalse();

    Optional<AssemblyOrderJpaEntity> findBySourceOrderIdAndRemovedFalse(UUID sourceOrderId);
}

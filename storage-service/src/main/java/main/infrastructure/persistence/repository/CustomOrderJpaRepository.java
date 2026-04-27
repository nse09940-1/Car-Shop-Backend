package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.CustomOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomOrderJpaRepository extends JpaRepository<CustomOrderJpaEntity, UUID> {
    Optional<CustomOrderJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<CustomOrderJpaEntity> findAllByRemovedFalse();

    List<CustomOrderJpaEntity> findAllByCustomerIdAndRemovedFalse(UUID customerId);

    boolean existsByIdAndCustomerIdAndRemovedFalse(UUID id, UUID customerId);
}


package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.StockOrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockOrderJpaRepository extends JpaRepository<StockOrderJpaEntity, UUID> {
    Optional<StockOrderJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<StockOrderJpaEntity> findAllByRemovedFalse();

    List<StockOrderJpaEntity> findAllByCustomerIdAndRemovedFalse(UUID customerId);

    boolean existsByIdAndCustomerIdAndRemovedFalse(UUID id, UUID customerId);
}


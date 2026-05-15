package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.CarJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarJpaRepository extends JpaRepository<CarJpaEntity, UUID> {
    Optional<CarJpaEntity> findByIdAndRemovedFalse(UUID id);

    Optional<CarJpaEntity> findByVinAndRemovedFalse(String vin);

    List<CarJpaEntity> findAllByRemovedFalse();

    List<CarJpaEntity> findAllByAvailableTrueAndRemovedFalse();

    Optional<CarJpaEntity> findByIdAndAvailableTrueAndRemovedFalse(UUID id);
}


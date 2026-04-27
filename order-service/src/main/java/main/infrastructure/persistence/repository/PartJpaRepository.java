package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.PartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartJpaRepository extends JpaRepository<PartJpaEntity, UUID> {
    Optional<PartJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<PartJpaEntity> findAllByRemovedFalse();
}


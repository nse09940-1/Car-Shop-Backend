package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.TestDriveJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestDriveJpaRepository extends JpaRepository<TestDriveJpaEntity, UUID> {
    Optional<TestDriveJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<TestDriveJpaEntity> findAllByRemovedFalse();
}


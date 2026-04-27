package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.SteeringOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SteeringOptionJpaRepository extends JpaRepository<SteeringOptionJpaEntity, UUID> {
    Optional<SteeringOptionJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<SteeringOptionJpaEntity> findAllByRemovedFalse();

    @Query("select o from SteeringOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and code = :modelCode")
    List<SteeringOptionJpaEntity> findByModelCode(String modelCode);

    @Query("select o from SteeringOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and o.baseOption = true and code = :modelCode")
    Optional<SteeringOptionJpaEntity> findBaseByModelCode(String modelCode);
}


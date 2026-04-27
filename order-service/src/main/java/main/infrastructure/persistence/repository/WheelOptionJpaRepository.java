package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.WheelOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WheelOptionJpaRepository extends JpaRepository<WheelOptionJpaEntity, UUID> {
    Optional<WheelOptionJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<WheelOptionJpaEntity> findAllByRemovedFalse();

    @Query("select o from WheelOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and code = :modelCode")
    List<WheelOptionJpaEntity> findByModelCode(String modelCode);

    @Query("select o from WheelOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and o.baseOption = true and code = :modelCode")
    Optional<WheelOptionJpaEntity> findBaseByModelCode(String modelCode);
}


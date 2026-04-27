package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.InteriorOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InteriorOptionJpaRepository extends JpaRepository<InteriorOptionJpaEntity, UUID> {
    Optional<InteriorOptionJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<InteriorOptionJpaEntity> findAllByRemovedFalse();

    @Query("select o from InteriorOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and code = :modelCode")
    List<InteriorOptionJpaEntity> findByModelCode(String modelCode);

    @Query("select o from InteriorOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and o.baseOption = true and code = :modelCode")
    Optional<InteriorOptionJpaEntity> findBaseByModelCode(String modelCode);
}


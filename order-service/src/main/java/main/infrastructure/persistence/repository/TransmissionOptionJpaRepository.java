package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.TransmissionOptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransmissionOptionJpaRepository extends JpaRepository<TransmissionOptionJpaEntity, UUID> {
    Optional<TransmissionOptionJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<TransmissionOptionJpaEntity> findAllByRemovedFalse();

    @Query("select o from TransmissionOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and code = :modelCode")
    List<TransmissionOptionJpaEntity> findByModelCode(String modelCode);

    @Query("select o from TransmissionOptionJpaEntity o join o.compatibleModelCodes code where o.removed = false and o.baseOption = true and code = :modelCode")
    Optional<TransmissionOptionJpaEntity> findBaseByModelCode(String modelCode);
}


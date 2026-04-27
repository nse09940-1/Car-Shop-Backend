package main.infrastructure.persistence.repository;

import main.infrastructure.persistence.entity.AssemblyOrderRequiredPartId;
import main.infrastructure.persistence.entity.AssemblyOrderRequiredPartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssemblyOrderRequiredPartJpaRepository extends JpaRepository<AssemblyOrderRequiredPartJpaEntity, AssemblyOrderRequiredPartId> {
    List<AssemblyOrderRequiredPartJpaEntity> findAllByAssemblyOrderId(UUID assemblyOrderId);

    void deleteAllByAssemblyOrderId(UUID assemblyOrderId);
}

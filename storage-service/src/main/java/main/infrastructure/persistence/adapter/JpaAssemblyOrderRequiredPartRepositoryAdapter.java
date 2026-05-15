package main.infrastructure.persistence.adapter;

import main.application.port.repository.AssemblyOrderRequiredPartRepository;
import main.domain.assembly.AssemblyRequiredPart;
import main.infrastructure.persistence.entity.AssemblyOrderRequiredPartJpaEntity;
import main.infrastructure.persistence.repository.AssemblyOrderRequiredPartJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JpaAssemblyOrderRequiredPartRepositoryAdapter implements AssemblyOrderRequiredPartRepository {
    private final AssemblyOrderRequiredPartJpaRepository jpaRepository;

    public JpaAssemblyOrderRequiredPartRepositoryAdapter(AssemblyOrderRequiredPartJpaRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "jpaRepository is required");
    }

    @Override
    public List<AssemblyRequiredPart> findAllByAssemblyOrderId(UUID assemblyOrderId) {
        return jpaRepository.findAllByAssemblyOrderId(assemblyOrderId).stream()
                .map(entity -> new AssemblyRequiredPart(entity.getPartId(), entity.getQuantity()))
                .toList();
    }

    @Override
    public void replaceAll(UUID assemblyOrderId, List<AssemblyRequiredPart> requiredParts) {
        jpaRepository.deleteAllByAssemblyOrderId(assemblyOrderId);
        if (requiredParts == null) {
            return;
        }
        for (AssemblyRequiredPart item : requiredParts) {
            AssemblyOrderRequiredPartJpaEntity entity = new AssemblyOrderRequiredPartJpaEntity();
            entity.setAssemblyOrderId(assemblyOrderId);
            entity.setPartId(item.partId());
            entity.setQuantity(item.quantity());
            jpaRepository.save(entity);
        }
    }
}

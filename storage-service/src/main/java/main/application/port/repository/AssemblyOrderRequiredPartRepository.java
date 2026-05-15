package main.application.port.repository;

import main.domain.assembly.AssemblyRequiredPart;

import java.util.List;
import java.util.UUID;

public interface AssemblyOrderRequiredPartRepository {
    List<AssemblyRequiredPart> findAllByAssemblyOrderId(UUID assemblyOrderId);

    void replaceAll(UUID assemblyOrderId, List<AssemblyRequiredPart> requiredParts);
}

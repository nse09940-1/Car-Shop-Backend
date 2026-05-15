package main.application.port.repository;

import main.domain.assembly.AssemblyOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssemblyOrderRepository {
    AssemblyOrder save(AssemblyOrder order);

    Optional<AssemblyOrder> findById(UUID id);

    List<AssemblyOrder> findAll();

    Optional<AssemblyOrder> findBySourceOrderId(UUID sourceOrderId);

    void deleteById(UUID id);
}

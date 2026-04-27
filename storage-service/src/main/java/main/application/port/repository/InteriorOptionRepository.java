package main.application.port.repository;

import main.domain.configuration.InteriorOption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InteriorOptionRepository {
    void save(InteriorOption option);

    Optional<InteriorOption> findById(UUID id);

    List<InteriorOption> findAll();

    List<InteriorOption> findByModelCode(String modelCode);

    Optional<InteriorOption> findBaseByModelCode(String modelCode);

    void deleteById(UUID id);
}

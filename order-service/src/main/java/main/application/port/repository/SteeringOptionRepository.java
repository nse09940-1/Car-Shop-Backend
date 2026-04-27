package main.application.port.repository;

import main.domain.configuration.SteeringOption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SteeringOptionRepository {
    void save(SteeringOption option);

    Optional<SteeringOption> findById(UUID id);

    List<SteeringOption> findAll();

    List<SteeringOption> findByModelCode(String modelCode);

    Optional<SteeringOption> findBaseByModelCode(String modelCode);

    void deleteById(UUID id);
}

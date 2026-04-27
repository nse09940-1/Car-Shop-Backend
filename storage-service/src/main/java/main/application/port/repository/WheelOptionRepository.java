package main.application.port.repository;

import main.domain.configuration.WheelOption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WheelOptionRepository {
    void save(WheelOption option);

    Optional<WheelOption> findById(UUID id);

    List<WheelOption> findAll();

    List<WheelOption> findByModelCode(String modelCode);

    Optional<WheelOption> findBaseByModelCode(String modelCode);

    void deleteById(UUID id);
}

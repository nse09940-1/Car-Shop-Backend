package main.application.port.repository;

import main.domain.configuration.TransmissionOption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransmissionOptionRepository {
    void save(TransmissionOption option);

    Optional<TransmissionOption> findById(UUID id);

    List<TransmissionOption> findAll();

    List<TransmissionOption> findByModelCode(String modelCode);

    Optional<TransmissionOption> findBaseByModelCode(String modelCode);

    void deleteById(UUID id);
}

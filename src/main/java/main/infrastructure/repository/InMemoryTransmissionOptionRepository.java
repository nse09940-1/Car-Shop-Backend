package main.infrastructure.repository;

import main.application.port.repository.TransmissionOptionRepository;
import main.domain.configuration.TransmissionOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryTransmissionOptionRepository implements TransmissionOptionRepository {
    private final Map<UUID, TransmissionOption> storage = new HashMap<>();

    @Override
    public void save(TransmissionOption option) {
        storage.put(option.spec().id(), option);
    }

    @Override
    public Optional<TransmissionOption> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<TransmissionOption> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<TransmissionOption> findByModelCode(String modelCode) {
        return storage.values().stream()
                .filter(o -> o.spec().isCompatibleWith(modelCode))
                .toList();
    }

    @Override
    public Optional<TransmissionOption> findBaseByModelCode(String modelCode) {
        return storage.values().stream()
                .filter(o -> o.spec().baseOption())
                .filter(o -> o.spec().isCompatibleWith(modelCode))
                .findFirst();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}

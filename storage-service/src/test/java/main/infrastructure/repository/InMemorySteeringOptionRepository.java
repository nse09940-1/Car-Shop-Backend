package main.infrastructure.repository;

import main.application.port.repository.SteeringOptionRepository;
import main.domain.configuration.SteeringOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemorySteeringOptionRepository implements SteeringOptionRepository {
    private final Map<UUID, SteeringOption> storage = new HashMap<>();

    @Override
    public void save(SteeringOption option) {
        storage.put(option.spec().id(), option);
    }

    @Override
    public Optional<SteeringOption> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<SteeringOption> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<SteeringOption> findByModelCode(String modelCode) {
        return storage.values().stream()
                .filter(o -> o.spec().isCompatibleWith(modelCode))
                .toList();
    }

    @Override
    public Optional<SteeringOption> findBaseByModelCode(String modelCode) {
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

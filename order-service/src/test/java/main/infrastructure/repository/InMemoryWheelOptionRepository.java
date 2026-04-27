package main.infrastructure.repository;

import main.application.port.repository.WheelOptionRepository;
import main.domain.configuration.WheelOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryWheelOptionRepository implements WheelOptionRepository {
    private final Map<UUID, WheelOption> storage = new HashMap<>();

    @Override
    public void save(WheelOption option) {
        storage.put(option.spec().id(), option);
    }

    @Override
    public Optional<WheelOption> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<WheelOption> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<WheelOption> findByModelCode(String modelCode) {
        return storage.values().stream()
                .filter(o -> o.spec().isCompatibleWith(modelCode))
                .toList();
    }

    @Override
    public Optional<WheelOption> findBaseByModelCode(String modelCode) {
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

package main.infrastructure.repository;

import main.application.port.repository.InteriorOptionRepository;
import main.domain.configuration.InteriorOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryInteriorOptionRepository implements InteriorOptionRepository {
    private final Map<UUID, InteriorOption> storage = new HashMap<>();

    @Override
    public void save(InteriorOption option) {
        storage.put(option.spec().id(), option);
    }

    @Override
    public Optional<InteriorOption> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<InteriorOption> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<InteriorOption> findByModelCode(String modelCode) {
        return storage.values().stream()
                .filter(o -> o.spec().isCompatibleWith(modelCode))
                .toList();
    }

    @Override
    public Optional<InteriorOption> findBaseByModelCode(String modelCode) {
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

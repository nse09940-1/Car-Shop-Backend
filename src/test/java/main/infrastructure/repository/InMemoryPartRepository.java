package main.infrastructure.repository;

import main.application.port.repository.PartRepository;
import main.domain.car.CarPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryPartRepository implements PartRepository {
    private final Map<UUID, CarPart> storage = new HashMap<>();

    @Override
    public void save(CarPart carPart) {
        storage.put(carPart.id(), carPart);
    }

    @Override
    public Optional<CarPart> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<CarPart> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


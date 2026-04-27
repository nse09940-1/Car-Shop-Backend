package main.infrastructure.repository;

import main.application.port.repository.CarModelRepository;
import main.domain.car.CarModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCarModelRepository implements CarModelRepository {
    private final Map<UUID, CarModel> storage = new HashMap<>();

    @Override
    public void save(CarModel carModel) {
        storage.put(carModel.id(), carModel);
    }

    @Override
    public Optional<CarModel> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<CarModel> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


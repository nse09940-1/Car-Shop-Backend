package main.infrastructure.repository;

import main.application.port.repository.CarRepository;
import main.domain.car.Car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCarRepository implements CarRepository {
    private final Map<UUID, Car> storage = new HashMap<>();

    @Override
    public void save(Car car) {
        storage.put(car.id(), car);
    }

    @Override
    public Optional<Car> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Car> findByVin(String vin) {
        return storage.values().stream().filter(car -> car.vin().equals(vin)).findFirst();
    }

    @Override
    public List<Car> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Car> findAllAvailable() {
        return storage.values().stream().filter(Car::available).toList();
    }

    @Override
    public Optional<Car> findAvailableById(UUID id) {
        return findById(id).filter(Car::available);
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


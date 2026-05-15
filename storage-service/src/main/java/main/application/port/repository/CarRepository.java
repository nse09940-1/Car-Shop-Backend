package main.application.port.repository;

import main.domain.car.Car;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarRepository {
    void save(Car car);

    Optional<Car> findById(UUID id);

    Optional<Car> findByVin(String vin);

    List<Car> findAll();

    List<Car> findAllAvailable();

    Optional<Car> findAvailableById(UUID id);

    void deleteById(UUID id);
}


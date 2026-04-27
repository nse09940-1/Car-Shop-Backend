package main.application.port.repository;

import main.domain.car.CarModel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarModelRepository {
    void save(CarModel carModel);

    Optional<CarModel> findById(UUID id);

    List<CarModel> findAll();

    void deleteById(UUID id);
}


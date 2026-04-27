package main.application.port.repository;

import main.domain.car.CarPart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartRepository {
    void save(CarPart carPart);

    Optional<CarPart> findById(UUID id);

    List<CarPart> findAll();

    void deleteById(UUID id);
}


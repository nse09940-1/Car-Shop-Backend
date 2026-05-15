package main.application.service;

import main.application.port.repository.CarRepository;
import main.domain.car.Car;
import main.domain.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class StorageCarQueryService {
    private final CarRepository carRepository;

    public StorageCarQueryService(CarRepository carRepository) {
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
    }

    public List<Car> findAvailableCars() {
        return carRepository.findAllAvailable();
    }

    public Car findAvailableCar(UUID id) {
        return carRepository.findAvailableById(id)
                .orElseThrow(() -> new EntityNotFoundException("Available car not found"));
    }

    public Car findCarById(UUID id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
    }
}

package main.presentation;

import main.application.port.repository.CarRepository;
import main.domain.exception.EntityNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/cars")
public class InternalCarController {
    private final CarRepository carRepository;

    public InternalCarController(CarRepository carRepository) {
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
    }

    @GetMapping("/{id}")
    public StorageCarResponse findById(@PathVariable UUID id) {
        var car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
        return new StorageCarResponse(car.id(), car.available(), car.availableForTestDrive());
    }

    public record StorageCarResponse(
            UUID id,
            boolean available,
            boolean availableForTestDrive) {
    }
}

package main.application.service;

import main.application.dto.CarCardDto;
import main.application.dto.CarFilterRequest;
import main.application.mapper.AppMapper;
import main.application.port.repository.CarRepository;
import main.application.port.repository.CarModelRepository;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.car.Car;
import main.domain.car.CarModel;
import main.domain.car.CarProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CarCatalogService {
    private final CarRepository carRepository;
    private final CarModelRepository carModelRepository;

    public CarCatalogService(CarRepository carRepository, CarModelRepository carModelRepository) {
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
        this.carModelRepository = Objects.requireNonNull(carModelRepository, "carModelRepository is required");
    }

    public List<CarCardDto> findAvailable(CarFilterRequest filter) {
        validateFilter(filter);
        Map<UUID, CarModel> modelsById = carModelRepository.findAll().stream()
                .collect(Collectors.toMap(CarModel::id, Function.identity()));

        return carRepository.findAll().stream()
                .filter(Car::available)
                .filter(car -> modelsById.containsKey(car.carModelId()))
                .filter(car -> matches(car, modelsById.get(car.carModelId()), filter))
                .map(car -> AppMapper.toDto(car, modelsById.get(car.carModelId())))
                .toList();
    }

    public CarCardDto findById(UUID id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
        CarModel carModel = carModelRepository.findById(car.carModelId())
                .orElseThrow(() -> new EntityNotFoundException("Car model not found"));
        return AppMapper.toDto(car, carModel);
    }

    private void validateFilter(CarFilterRequest filter) {
        if (filter == null) return;
        if (filter.model() != null && filter.brand() == null) {
            throw new DomainValidationException("Brand is required for model filter");
        }
    }

    private boolean matches(Car car, CarModel model, CarFilterRequest f) {
        if (f == null) return true;
        CarProperty specs = model.properties();
        return (f.priceMin() == null || car.price().rubles() >= f.priceMin().rubles())
                && (f.priceMax() == null || car.price().rubles() <= f.priceMax().rubles())
                && (f.brand() == null || f.brand().equalsIgnoreCase(model.brand()))
                && (f.model() == null || f.model().equalsIgnoreCase(model.modelCode()))
                && (f.color() == null || f.color().equalsIgnoreCase(car.color()))
                && (f.carType() == null || f.carType() == specs.carType())
                && (f.fuelType() == null || f.fuelType() == specs.fuelType())
                && (f.transmissionType() == null || f.transmissionType() == specs.transmissionType())
                && (f.driveType() == null || f.driveType() == specs.driveType())
                && (f.horsepowerMin() == null || specs.engine().horsepower() >= f.horsepowerMin())
                && (f.horsepowerMax() == null || specs.engine().horsepower() <= f.horsepowerMax())
                && (f.engineVolumeMin() == null || specs.engine().engineVolume() >= f.engineVolumeMin())
                && (f.engineVolumeMax() == null || specs.engine().engineVolume() <= f.engineVolumeMax());
    }
}

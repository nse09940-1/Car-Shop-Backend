package main.application.service;

import main.application.dto.CarCardDto;
import main.application.dto.CustomOrderDto;
import main.application.dto.PartDto;
import main.application.dto.StockOrderDto;
import main.application.dto.TestDriveDto;
import main.application.mapper.AppMapper;
import main.application.port.repository.CarRepository;
import main.application.port.repository.CarModelRepository;
import main.application.port.repository.CustomOrderRepository;
import main.application.port.repository.InteriorOptionRepository;
import main.application.port.repository.PartRepository;
import main.application.port.repository.SteeringOptionRepository;
import main.application.port.repository.StockOrderRepository;
import main.application.port.repository.TestDriveRepository;
import main.application.port.repository.TransmissionOptionRepository;
import main.application.port.repository.UserRepository;
import main.application.port.repository.WheelOptionRepository;
import main.domain.car.Car;
import main.domain.car.CarModel;
import main.domain.car.CarPart;
import main.domain.configuration.InteriorOption;
import main.domain.configuration.SteeringOption;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.WheelOption;
import main.domain.configuration.ConfigType;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.user.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SystemAdminService {
    private final CarModelRepository carModelRepository;
    private final CarRepository carRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final WheelOptionRepository wheelOptionRepository;
    private final TransmissionOptionRepository transmissionOptionRepository;
    private final SteeringOptionRepository steeringOptionRepository;
    private final InteriorOptionRepository interiorOptionRepository;
    private final StockOrderRepository stockOrderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final TestDriveRepository testDriveRepository;

    public SystemAdminService(
            CarModelRepository carModelRepository,
            CarRepository carRepository,
            PartRepository partRepository,
            UserRepository userRepository,
            WheelOptionRepository wheelOptionRepository,
            TransmissionOptionRepository transmissionOptionRepository,
            SteeringOptionRepository steeringOptionRepository,
            InteriorOptionRepository interiorOptionRepository,
            StockOrderRepository stockOrderRepository,
            CustomOrderRepository customOrderRepository,
            TestDriveRepository testDriveRepository) {
        this.carModelRepository = Objects.requireNonNull(carModelRepository, "carModelRepository is required");
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository is required");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
        this.wheelOptionRepository = Objects.requireNonNull(wheelOptionRepository, "wheelOptionRepository is required");
        this.transmissionOptionRepository = Objects.requireNonNull(transmissionOptionRepository, "transmissionOptionRepository is required");
        this.steeringOptionRepository = Objects.requireNonNull(steeringOptionRepository, "steeringOptionRepository is required");
        this.interiorOptionRepository = Objects.requireNonNull(interiorOptionRepository, "interiorOptionRepository is required");
        this.stockOrderRepository = Objects.requireNonNull(stockOrderRepository, "stockOrderRepository is required");
        this.customOrderRepository = Objects.requireNonNull(customOrderRepository, "customOrderRepository is required");
        this.testDriveRepository = Objects.requireNonNull(testDriveRepository, "testDriveRepository is required");
    }

    
    public void saveCarModel(CarModel model) {
        carModelRepository.save(model);
    }

    public List<CarModel> listCarModels() {
        return carModelRepository.findAll();
    }

    public void deleteCarModel(UUID id) {
        carModelRepository.deleteById(id);
    }

    
    public void saveCar(Car car) {
        carRepository.save(car);
    }

    public List<CarCardDto> listCars() {
        return carRepository.findAll().stream()
                .map(car -> {
                    CarModel model = carModelRepository.findById(car.carModelId())
                            .orElseThrow(() -> new EntityNotFoundException("Car model not found"));
                    return AppMapper.toDto(car, model);
                })
                .toList();
    }

    public void deleteCar(UUID id) {
        carRepository.deleteById(id);
    }

    
    public void savePart(CarPart carPart) {
        partRepository.save(carPart);
    }

    public List<PartDto> listParts() {
        return partRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    public void deletePart(UUID id) {
        partRepository.deleteById(id);
    }

    
    public void saveUser(User user) {
        userRepository.save(user);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    
    public void saveWheelOption(WheelOption option) {
        validateOptionPartType(option.spec().carPartId(), ConfigType.WHEELS);
        wheelOptionRepository.save(option);
    }

    public List<WheelOption> listWheelOptions() {
        return wheelOptionRepository.findAll();
    }

    public void deleteWheelOption(UUID id) {
        wheelOptionRepository.deleteById(id);
    }

    public void saveTransmissionOption(TransmissionOption option) {
        validateOptionPartType(option.spec().carPartId(), ConfigType.TRANSMISSION);
        transmissionOptionRepository.save(option);
    }

    public List<TransmissionOption> listTransmissionOptions() {
        return transmissionOptionRepository.findAll();
    }

    public void deleteTransmissionOption(UUID id) {
        transmissionOptionRepository.deleteById(id);
    }

    public void saveSteeringOption(SteeringOption option) {
        validateOptionPartType(option.spec().carPartId(), ConfigType.STEERING);
        steeringOptionRepository.save(option);
    }

    public List<SteeringOption> listSteeringOptions() {
        return steeringOptionRepository.findAll();
    }

    public void deleteSteeringOption(UUID id) {
        steeringOptionRepository.deleteById(id);
    }

    public void saveInteriorOption(InteriorOption option) {
        validateOptionPartType(option.spec().carPartId(), ConfigType.INTERIOR);
        interiorOptionRepository.save(option);
    }

    public List<InteriorOption> listInteriorOptions() {
        return interiorOptionRepository.findAll();
    }

    public void deleteInteriorOption(UUID id) {
        interiorOptionRepository.deleteById(id);
    }


    public List<StockOrderDto> listStockOrders() {
        return stockOrderRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    public void deleteStockOrder(UUID id) {
        stockOrderRepository.deleteById(id);
    }

    
    public List<CustomOrderDto> listCustomOrders() {
        return customOrderRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    public void deleteCustomOrder(UUID id) {
        customOrderRepository.deleteById(id);
    }

    public List<TestDriveDto> listTestDrives() {
        return testDriveRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    public void deleteTestDrive(UUID id) {
        testDriveRepository.deleteById(id);
    }

    private void validateOptionPartType(UUID partId, ConfigType expectedType) {
        CarPart part = partRepository.findById(partId)
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));
        if (part.partType() != expectedType) {
            throw new DomainValidationException(
                    "Part type mismatch: expected " + expectedType + ", actual " + part.partType());
        }
    }
}

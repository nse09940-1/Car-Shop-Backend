package main.application.service;

import main.application.dto.CarCardDto;
import main.application.dto.TestDriveDto;
import main.application.mapper.AppMapper;
import main.application.port.repository.CarRepository;
import main.application.port.repository.CarModelRepository;
import main.application.port.repository.TestDriveRepository;
import main.application.port.repository.UserRepository;
import main.application.port.security.CurrentUserProvider;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.car.Car;
import main.domain.car.CarModel;
import main.domain.TestDriveRequest;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.security.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TestDriveService {
    private final TestDriveRepository testDriveRepository;
    private final CarRepository carRepository;
    private final CarModelRepository carModelRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public TestDriveService(
            TestDriveRepository testDriveRepository,
            CarRepository carRepository,
            CarModelRepository carModelRepository,
            UserRepository userRepository,
            CurrentUserProvider currentUserProvider) {
        this.testDriveRepository = Objects.requireNonNull(testDriveRepository, "testDriveRepository is required");
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
        this.carModelRepository = Objects.requireNonNull(carModelRepository, "carModelRepository is required");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider is required");
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TestDriveDto createRequest(UUID carId, LocalDateTime scheduledAt) {
        User customer = userRepository.findById(currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        if (!currentUserProvider.hasRole(SecurityRoles.ADMIN) && customer.role() != Role.CLIENT) {
            throw new DomainValidationException("Only client can request test drive");
        }
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            throw new DomainValidationException("Date must be in the future");
        }
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
        if (!car.available()) {
            throw new DomainValidationException("Car is not available");
        }
        if (!car.availableForTestDrive()) {
            throw new DomainValidationException("Car is not in test-drive list");
        }
        TestDriveRequest testDriveRequest = new TestDriveRequest(
                UUID.randomUUID(),
                customer.id(),
                car.id(),
                scheduledAt);
        testDriveRepository.save(testDriveRequest);
        return AppMapper.toDto(testDriveRequest);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<TestDriveDto> findAll() {
        return testDriveRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void addCarToTestDriveList(UUID carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
        carRepository.save(car.withAvailableForTestDrive(true));
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public void removeCarFromTestDriveList(UUID carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
        carRepository.save(car.withAvailableForTestDrive(false));
    }

    @PreAuthorize("isAuthenticated()")
    public List<CarCardDto> getTestDriveCars() {
        return carRepository.findAll().stream()
                .filter(Car::availableForTestDrive)
                .map(car -> {
                    CarModel model = carModelRepository.findById(car.carModelId())
                            .orElseThrow(() -> new EntityNotFoundException("Car model not found"));
                    return AppMapper.toDto(car, model);
                })
                .toList();
    }
}

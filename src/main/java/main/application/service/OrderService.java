package main.application.service;

import main.application.dto.CustomOrderDto;
import main.application.dto.StockOrderDto;
import main.application.mapper.AppMapper;
import main.application.port.policy.ManagerAssignmentPolicy;
import main.application.port.policy.OrderTransitionPolicy;
import main.application.port.repository.CarRepository;
import main.application.port.repository.CustomOrderRepository;
import main.application.port.repository.PartRepository;
import main.application.port.repository.StockOrderRepository;
import main.application.port.repository.UserRepository;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.car.Car;
import main.domain.car.CarPart;
import main.domain.configuration.CarConfiguration;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.domain.configuration.ConfigType;
import main.domain.user.Role;
import main.domain.user.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderService {
    private final StockOrderRepository stockOrderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final CarRepository carRepository;
    private final PartRepository partRepository;
    private final UserRepository userRepository;
    private final ManagerAssignmentPolicy managerAssignmentPolicy;
    private final OrderTransitionPolicy orderTransitionPolicy;
    private final ConfiguratorService configuratorService;

    public OrderService(
            StockOrderRepository stockOrderRepository,
            CustomOrderRepository customOrderRepository,
            CarRepository carRepository,
            PartRepository partRepository,
            UserRepository userRepository,
            ManagerAssignmentPolicy managerAssignmentPolicy,
            OrderTransitionPolicy orderTransitionPolicy,
            ConfiguratorService configuratorService) {
        this.stockOrderRepository = Objects.requireNonNull(stockOrderRepository, "stockOrderRepository is required");
        this.customOrderRepository = Objects.requireNonNull(customOrderRepository, "customOrderRepository is required");
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository is required");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
        this.managerAssignmentPolicy = Objects.requireNonNull(managerAssignmentPolicy, "managerAssignmentPolicy is required");
        this.orderTransitionPolicy = Objects.requireNonNull(orderTransitionPolicy, "orderTransitionPolicy is required");
        this.configuratorService = Objects.requireNonNull(configuratorService, "configuratorService is required");
    }

    public StockOrderDto createStockOrder(UUID customerId, UUID carId) {
        User customer = getValidatedClient(customerId);
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));
        if (!car.available()) {
            throw new DomainValidationException("Car is not available");
        }
        User assignedManager = assignManager();

        StockCarOrder order = new StockCarOrder(
                UUID.randomUUID(),
                customer.id(),
                assignedManager.id(),
                LocalDateTime.now(),
                car.id(),
                StockOrderStatus.CREATED);

        carRepository.save(car.withAvailable(false));
        stockOrderRepository.save(order);
        return AppMapper.toDto(order);
    }

    public StockOrderDto changeStockStatus(UUID orderId, StockOrderStatus newStatus) {
        StockCarOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        assertTransition(order.status(), newStatus);
        order.setStatus(newStatus);
        if (newStatus == StockOrderStatus.CANCELLED) {
            carRepository.findById(order.carId())
                    .ifPresent(car -> carRepository.save(car.withAvailable(true)));
        }
        stockOrderRepository.save(order);
        return AppMapper.toDto(order);
    }

    public StockOrderDto findStockById(UUID id) {
        StockCarOrder order = stockOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        return AppMapper.toDto(order);
    }

    public List<StockOrderDto> findAllStock() {
        return stockOrderRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    public CustomOrderDto createCustomOrder(UUID customerId, UUID carModelId, Map<ConfigType, UUID> selectedOptionIds) {
        
        User customer = getValidatedClient(customerId);
        User assignedManager = assignManager();

        ConfiguratorService.BuiltConfiguration builtConfiguration =
                configuratorService.buildDomainConfiguration(carModelId, selectedOptionIds);

        CustomCarOrder order = new CustomCarOrder(
                UUID.randomUUID(),
                customer.id(),
                assignedManager.id(),
                LocalDateTime.now(),
                carModelId,
                builtConfiguration.configuration(),
                builtConfiguration.totalPrice(),
                CustomOrderStatus.CREATED);
        customOrderRepository.save(order);
        return AppMapper.toDto(order);
    }

    public CustomOrderDto changeCustomStatus(UUID orderId, CustomOrderStatus newStatus) {
        CustomCarOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));

        CustomOrderStatus currentStatus = order.status();
        assertTransition(currentStatus, newStatus);

        if (currentStatus == CustomOrderStatus.CREATED && newStatus == CustomOrderStatus.WAREHOUSE_APPROVED) {
            reserveParts(order);
        }
        if (newStatus == CustomOrderStatus.CANCELLED && currentStatus != CustomOrderStatus.CREATED) {
            releaseParts(order);
        }
        if (currentStatus == CustomOrderStatus.READY_FOR_HANDOVER && newStatus == CustomOrderStatus.COMPLETED) {
            consumeParts(order);
        }

        order.setStatus(newStatus);
        customOrderRepository.save(order);
        return AppMapper.toDto(order);
    }

    public CustomOrderDto findCustomById(UUID id) {
        CustomCarOrder order = customOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
        return AppMapper.toDto(order);
    }

    public List<CustomOrderDto> findAllCustom() {
        return customOrderRepository.findAll().stream().map(AppMapper::toDto).toList();
    }

    private User getValidatedClient(UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        if (customer.role() != Role.CLIENT) {
            throw new DomainValidationException("Only client can create order");
        }
        return customer;
    }

    private User assignManager() {
        List<User> managers = userRepository.findByRole(Role.MANAGER);
        return managerAssignmentPolicy.assignManager(managers);
    }

    private void assertTransition(StockOrderStatus from, StockOrderStatus to) {
        if (!orderTransitionPolicy.canTransition(from, to)) {
            throw new DomainValidationException("Invalid stock status change");
        }
    }

    private void assertTransition(CustomOrderStatus from, CustomOrderStatus to) {
        if (!orderTransitionPolicy.canTransition(from, to)) {
            throw new DomainValidationException("Invalid custom status change");
        }
    }

    private void reserveParts(CustomCarOrder order) {
        CarConfiguration configuration = order.configuration();
        reserveOne(configuration.wheelOption() == null ? null : configuration.wheelOption().spec().carPartId(), ConfigType.WHEELS);
        reserveOne(configuration.transmissionOption() == null ? null : configuration.transmissionOption().spec().carPartId(), ConfigType.TRANSMISSION);
        reserveOne(configuration.steeringOption() == null ? null : configuration.steeringOption().spec().carPartId(), ConfigType.STEERING);
        reserveOne(configuration.interiorOption() == null ? null : configuration.interiorOption().spec().carPartId(), ConfigType.INTERIOR);
    }

    private void releaseParts(CustomCarOrder order) {
        CarConfiguration configuration = order.configuration();
        releaseOne(configuration.wheelOption() == null ? null : configuration.wheelOption().spec().carPartId(), ConfigType.WHEELS);
        releaseOne(configuration.transmissionOption() == null ? null : configuration.transmissionOption().spec().carPartId(), ConfigType.TRANSMISSION);
        releaseOne(configuration.steeringOption() == null ? null : configuration.steeringOption().spec().carPartId(), ConfigType.STEERING);
        releaseOne(configuration.interiorOption() == null ? null : configuration.interiorOption().spec().carPartId(), ConfigType.INTERIOR);
    }

    private void consumeParts(CustomCarOrder order) {
        CarConfiguration configuration = order.configuration();
        consumeOne(configuration.wheelOption() == null ? null : configuration.wheelOption().spec().carPartId(), ConfigType.WHEELS);
        consumeOne(configuration.transmissionOption() == null ? null : configuration.transmissionOption().spec().carPartId(), ConfigType.TRANSMISSION);
        consumeOne(configuration.steeringOption() == null ? null : configuration.steeringOption().spec().carPartId(), ConfigType.STEERING);
        consumeOne(configuration.interiorOption() == null ? null : configuration.interiorOption().spec().carPartId(), ConfigType.INTERIOR);
    }

    private void reserveOne(UUID partId, ConfigType expectedType) {
        if (partId == null) return;
        CarPart part = getAndValidatePart(partId, expectedType);
        partRepository.save(part.reserve(1));
    }

    private void releaseOne(UUID partId, ConfigType expectedType) {
        if (partId == null) return;
        CarPart part = getAndValidatePart(partId, expectedType);
        partRepository.save(part.release(1));
    }

    private void consumeOne(UUID partId, ConfigType expectedType) {
        if (partId == null) return;
        CarPart part = getAndValidatePart(partId, expectedType);
        partRepository.save(part.consume(1));
    }

    private CarPart getAndValidatePart(UUID partId, ConfigType expectedType) {
        CarPart part = partRepository.findById(partId)
                .orElseThrow(() -> new EntityNotFoundException("Part not found"));
        assertPartType(part, expectedType);
        return part;
    }

    private void assertPartType(CarPart part, ConfigType expectedType) {
        if (part.partType() != expectedType) {
            throw new DomainValidationException(
                    "Part type mismatch ");
        }
    }

}

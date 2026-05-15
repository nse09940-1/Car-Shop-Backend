package main.application.service;

import contracts.events.OrderType;
import contracts.events.RequiredPartItem;
import main.application.dto.CustomOrderDto;
import main.application.dto.StockOrderDto;
import main.application.mapper.AppMapper;
import main.application.port.policy.ManagerAssignmentPolicy;
import main.application.port.policy.OrderTransitionPolicy;
import main.application.port.repository.CustomOrderRepository;
import main.application.port.repository.StockOrderRepository;
import main.application.port.repository.UserRepository;
import main.application.port.security.CurrentUserProvider;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.messaging.OrderOutboxService;
import main.infrastructure.security.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderService {
    private final StockOrderRepository stockOrderRepository;
    private final CustomOrderRepository customOrderRepository;
    private final UserRepository userRepository;
    private final ManagerAssignmentPolicy managerAssignmentPolicy;
    private final OrderTransitionPolicy orderTransitionPolicy;
    private final ConfiguratorService configuratorService;
    private final CurrentUserProvider currentUserProvider;
    private final OrderOutboxService orderOutboxService;

    public OrderService(
            StockOrderRepository stockOrderRepository,
            CustomOrderRepository customOrderRepository,
            UserRepository userRepository,
            ManagerAssignmentPolicy managerAssignmentPolicy,
            OrderTransitionPolicy orderTransitionPolicy,
            ConfiguratorService configuratorService,
            CurrentUserProvider currentUserProvider,
            OrderOutboxService orderOutboxService) {
        this.stockOrderRepository = Objects.requireNonNull(stockOrderRepository, "stockOrderRepository is required");
        this.customOrderRepository = Objects.requireNonNull(customOrderRepository, "customOrderRepository is required");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
        this.managerAssignmentPolicy = Objects.requireNonNull(managerAssignmentPolicy, "managerAssignmentPolicy is required");
        this.orderTransitionPolicy = Objects.requireNonNull(orderTransitionPolicy, "orderTransitionPolicy is required");
        this.configuratorService = Objects.requireNonNull(configuratorService, "configuratorService is required");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider is required");
        this.orderOutboxService = Objects.requireNonNull(orderOutboxService, "orderOutboxService is required");
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public StockOrderDto createStockOrder(UUID carId) {
        User customer = getValidatedCurrentUser();
        User assignedManager = assignManager();

        StockCarOrder order = new StockCarOrder(
                UUID.randomUUID(),
                customer.id(),
                assignedManager.id(),
                LocalDateTime.now(),
                carId,
                StockOrderStatus.CREATED);

        stockOrderRepository.save(order);
        orderOutboxService.enqueueStockCarReservationRequested(order);
        return AppMapper.toDto(order);
    }

    @PreAuthorize("@orderAccess.canChangeStockStatus(#orderId, #newStatus)")
    @Transactional
    public StockOrderDto changeStockStatus(UUID orderId, StockOrderStatus newStatus) {
        StockCarOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        StockOrderStatus currentStatus = order.status();
        assertTransition(order.status(), newStatus);
        order.setStatus(newStatus);
        stockOrderRepository.save(order);
        if (newStatus == StockOrderStatus.PAID) {
            orderOutboxService.enqueueStockCarWriteOffRequested(order);
        }
        if (newStatus == StockOrderStatus.CANCELLED
                && (currentStatus == StockOrderStatus.MANAGER_APPROVED
                || currentStatus == StockOrderStatus.AWAITING_PAYMENT
                || currentStatus == StockOrderStatus.PAID)) {
            orderOutboxService.enqueueStockCarReservationReleaseRequested(order);
        }
        return AppMapper.toDto(order);
    }

    @PreAuthorize("@orderAccess.canAccessStockOrder(#id)")
    public StockOrderDto findStockById(UUID id) {
        StockCarOrder order = stockOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        return AppMapper.toDto(order);
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<StockOrderDto> findAllStock() {
        List<StockCarOrder> orders = currentUserProvider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.MANAGER)
                ? stockOrderRepository.findAll()
                : stockOrderRepository.findAllByCustomerId(currentUserProvider.currentUserId());
        return orders.stream().map(AppMapper::toDto).toList();
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public CustomOrderDto createCustomOrder(UUID carModelId, Map<ConfigType, UUID> selectedOptionIds) {
        User customer = getValidatedCurrentUser();
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
        orderOutboxService.enqueueCustomCarPartsReservationRequested(order, buildRequiredParts(order));
        return AppMapper.toDto(order);
    }

    @PreAuthorize("@orderAccess.canChangeCustomStatus(#orderId, #newStatus)")
    @Transactional
    public CustomOrderDto changeCustomStatus(UUID orderId, CustomOrderStatus newStatus) {
        CustomCarOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));

        CustomOrderStatus currentStatus = order.status();
        assertTransition(currentStatus, newStatus);

        order.setStatus(newStatus);
        customOrderRepository.save(order);
        if (newStatus == CustomOrderStatus.PAID) {
            orderOutboxService.enqueueCustomExecutionRequested(order);
        }
        if (newStatus == CustomOrderStatus.CANCELLED
                && (currentStatus == CustomOrderStatus.CREATED
                || currentStatus == CustomOrderStatus.WAREHOUSE_APPROVED
                || currentStatus == CustomOrderStatus.AWAITING_PAYMENT)) {
            orderOutboxService.enqueueCustomReservationReleaseRequested(order);
        }
        return AppMapper.toDto(order);
    }

    @PreAuthorize("@orderAccess.canAccessCustomOrder(#id)")
    public CustomOrderDto findCustomById(UUID id) {
        CustomCarOrder order = customOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
        return AppMapper.toDto(order);
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<CustomOrderDto> findAllCustom() {
        List<CustomCarOrder> orders = currentUserProvider.hasAnyRole(SecurityRoles.ADMIN, SecurityRoles.MANAGER)
                ? customOrderRepository.findAll()
                : customOrderRepository.findAllByCustomerId(currentUserProvider.currentUserId());
        return orders.stream().map(AppMapper::toDto).toList();
    }

    @Transactional
    public void markOrderReadyForHandover(UUID orderId, OrderType orderType) {
        switch (orderType) {
            case STOCK -> {
                StockCarOrder order = stockOrderRepository.findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
                if (order.status() == StockOrderStatus.AWAITING_DELIVERY) {
                    order.setStatus(StockOrderStatus.READY_FOR_HANDOVER);
                    stockOrderRepository.save(order);
                }
            }
            case CUSTOM -> {
                CustomCarOrder order = customOrderRepository.findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
                if (order.status() == CustomOrderStatus.AWAITING_DELIVERY) {
                    order.setStatus(CustomOrderStatus.READY_FOR_HANDOVER);
                    customOrderRepository.save(order);
                }
            }
        }
    }

    @Transactional
    public void rejectOrder(UUID orderId, OrderType orderType) {
        switch (orderType) {
            case STOCK -> {
                StockCarOrder order = stockOrderRepository.findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
                if (order.status() == StockOrderStatus.CREATED
                        || order.status() == StockOrderStatus.PAID) {
                    order.setStatus(StockOrderStatus.CANCELLED);
                    stockOrderRepository.save(order);
                }
            }
            case CUSTOM -> {
                CustomCarOrder order = customOrderRepository.findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
                if (order.status() == CustomOrderStatus.PAID) {
                    order.setStatus(CustomOrderStatus.CANCELLED);
                    customOrderRepository.save(order);
                }
            }
        }
    }

    @Transactional
    public void markStockCarReserved(UUID orderId) {
        StockCarOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        if (order.status() == StockOrderStatus.CREATED) {
            order.setStatus(StockOrderStatus.MANAGER_APPROVED);
            stockOrderRepository.save(order);
        }
    }

    @Transactional
    public void rejectStockCarReservation(UUID orderId) {
        StockCarOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        if (order.status() == StockOrderStatus.CREATED) {
            order.setStatus(StockOrderStatus.CANCELLED);
            stockOrderRepository.save(order);
        }
    }

    @Transactional
    public void markStockCarWrittenOff(UUID orderId) {
        StockCarOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        if (order.status() == StockOrderStatus.PAID) {
            order.setStatus(StockOrderStatus.AWAITING_DELIVERY);
            stockOrderRepository.save(order);
        }
    }

    @Transactional
    public void rejectStockCarWriteOff(UUID orderId) {
        StockCarOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Stock order not found"));
        if (order.status() == StockOrderStatus.PAID) {
            order.setStatus(StockOrderStatus.CANCELLED);
            stockOrderRepository.save(order);
        }
    }

    @Transactional
    public void approveCustomOrder(UUID orderId) {
        CustomCarOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
        if (order.status() == CustomOrderStatus.CREATED) {
            order.setStatus(CustomOrderStatus.WAREHOUSE_APPROVED);
            customOrderRepository.save(order);
        }
    }

    @Transactional
    public void rejectCustomOrderOnCreate(UUID orderId) {
        CustomCarOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
        if (order.status() == CustomOrderStatus.CREATED) {
            order.setStatus(CustomOrderStatus.CANCELLED);
            customOrderRepository.save(order);
        }
    }

    @Transactional
    public void markCustomOrderAwaitingDelivery(UUID orderId) {
        CustomCarOrder order = customOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Custom order not found"));
        if (order.status() == CustomOrderStatus.PAID) {
            order.setStatus(CustomOrderStatus.AWAITING_DELIVERY);
            customOrderRepository.save(order);
        }
    }

    public List<RequiredPartItem> buildRequiredParts(CustomCarOrder order) {
        CarConfiguration configuration = order.configuration();
        return Stream.of(
                        configuration.wheelOption() == null ? null : configuration.wheelOption().spec().carPartId(),
                        configuration.transmissionOption() == null ? null : configuration.transmissionOption().spec().carPartId(),
                        configuration.steeringOption() == null ? null : configuration.steeringOption().spec().carPartId(),
                        configuration.interiorOption() == null ? null : configuration.interiorOption().spec().carPartId())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(partId -> partId, Collectors.summingInt(partId -> 1)))
                .entrySet().stream()
                .map(entry -> new RequiredPartItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private User getValidatedCurrentUser() {
        User customer = userRepository.findById(currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        if (!currentUserProvider.hasRole(SecurityRoles.ADMIN) && customer.role() != Role.CLIENT) {
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
}

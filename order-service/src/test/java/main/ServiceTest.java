package main;

import contracts.events.RequiredPartItem;
import main.application.dto.CarConfigurationDto;
import main.application.dto.ConfigurationPriceDto;
import main.application.dto.CustomOrderDto;
import main.application.dto.StockOrderDto;
import main.application.dto.TestDriveDto;
import main.application.port.client.StorageReadClient;
import main.application.port.security.CurrentUserProvider;
import main.application.service.ConfiguratorService;
import main.application.service.OrderService;
import main.application.service.SystemAdminService;
import main.application.service.TestDriveService;
import main.domain.Money;
import main.domain.car.CarModel;
import main.domain.car.CarProperty;
import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.Engine;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import main.domain.configuration.ConfigType;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.WheelOption;
import main.domain.exception.DomainValidationException;
import main.domain.exception.IncompatibleComponentException;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockOrderStatus;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.messaging.OrderOutboxService;
import main.infrastructure.policy.DefaultCompatibilityPolicy;
import main.infrastructure.policy.DefaultOrderTransitionPolicy;
import main.infrastructure.policy.RandomManagerAssignmentPolicy;
import main.infrastructure.repository.InMemoryCarModelRepository;
import main.infrastructure.repository.InMemoryCustomOrderRepository;
import main.infrastructure.repository.InMemoryInteriorOptionRepository;
import main.infrastructure.repository.InMemorySteeringOptionRepository;
import main.infrastructure.repository.InMemoryStockOrderRepository;
import main.infrastructure.repository.InMemoryTestDriveRepository;
import main.infrastructure.repository.InMemoryTransmissionOptionRepository;
import main.infrastructure.repository.InMemoryUserRepository;
import main.infrastructure.repository.InMemoryWheelOptionRepository;
import main.infrastructure.security.SecurityRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Nested
    class ConfiguratorTests {
        InMemoryCarModelRepository modelRepo;
        InMemoryWheelOptionRepository wheelRepo;
        InMemoryTransmissionOptionRepository transmissionRepo;
        InMemorySteeringOptionRepository steeringRepo;
        InMemoryInteriorOptionRepository interiorRepo;
        ConfiguratorService service;

        @BeforeEach
        void setup() {
            modelRepo = new InMemoryCarModelRepository();
            wheelRepo = new InMemoryWheelOptionRepository();
            transmissionRepo = new InMemoryTransmissionOptionRepository();
            steeringRepo = new InMemorySteeringOptionRepository();
            interiorRepo = new InMemoryInteriorOptionRepository();
            service = new ConfiguratorService(modelRepo, wheelRepo, transmissionRepo, steeringRepo, interiorRepo,
                    new DefaultCompatibilityPolicy());
        }

        @Test
        void getBaseConfiguration_returnsBasicConfig() {
            UUID modelId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(UUID.randomUUID(), "Base Wheels", new Money(0), Set.of("320i"), UUID.randomUUID(), true),
                    17));

            CarConfigurationDto dto = service.getBaseConfiguration(modelId);

            assertEquals("Base Wheels", dto.selectedOptions().get(ConfigType.WHEELS));
        }

        @Test
        void buildConfiguration_validAndInvalid() {
            UUID modelId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optionId, "Sport Wheels", new Money(50_000), Set.of("320i"), UUID.randomUUID(), false),
                    19));

            ConfigurationPriceDto result = service.buildConfiguration(modelId, Map.of(ConfigType.WHEELS, optionId));
            assertEquals(3_050_000, result.totalPrice().rubles());

            UUID incompatibleId = UUID.randomUUID();
            wheelRepo.save(new WheelOption(
                    new OptionSpec(incompatibleId, "X5 Wheels", new Money(50_000), Set.of("X5"), UUID.randomUUID(), false),
                    19));
            assertThrows(IncompatibleComponentException.class,
                    () -> service.buildConfiguration(modelId, Map.of(ConfigType.WHEELS, incompatibleId)));
        }
    }

    @Nested
    class OrderTests {
        InMemoryStockOrderRepository stockRepo;
        InMemoryCustomOrderRepository customRepo;
        InMemoryUserRepository userRepo;
        InMemoryCarModelRepository modelRepo;
        InMemoryWheelOptionRepository wheelRepo;
        InMemoryTransmissionOptionRepository transmissionRepo;
        InMemorySteeringOptionRepository steeringRepo;
        InMemoryInteriorOptionRepository interiorRepo;
        StubCurrentUserProvider currentUserProvider;
        OrderOutboxService outboxService;
        OrderService service;

        @BeforeEach
        void setup() {
            stockRepo = new InMemoryStockOrderRepository();
            customRepo = new InMemoryCustomOrderRepository();
            userRepo = new InMemoryUserRepository();
            modelRepo = new InMemoryCarModelRepository();
            wheelRepo = new InMemoryWheelOptionRepository();
            transmissionRepo = new InMemoryTransmissionOptionRepository();
            steeringRepo = new InMemorySteeringOptionRepository();
            interiorRepo = new InMemoryInteriorOptionRepository();
            currentUserProvider = new StubCurrentUserProvider();
            outboxService = mock(OrderOutboxService.class);

            var configurator = new ConfiguratorService(modelRepo, wheelRepo, transmissionRepo, steeringRepo, interiorRepo,
                    new DefaultCompatibilityPolicy());
            service = new OrderService(stockRepo, customRepo, userRepo,
                    new RandomManagerAssignmentPolicy(new Random(42)),
                    new DefaultOrderTransitionPolicy(),
                    configurator,
                    currentUserProvider,
                    outboxService);
        }

        private User addClient() {
            User client = new User(UUID.randomUUID(), "Client", "c@m.ru", Role.CLIENT);
            userRepo.save(client);
            return client;
        }

        private void addManager() {
            userRepo.save(new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER));
        }

        @Test
        void stockOrderLifecycle_enqueuesOutboxOnPaid() {
            User client = addClient();
            addManager();
            UUID carId = UUID.randomUUID();
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            StockOrderDto dto = service.createStockOrder(carId);
            assertEquals(StockOrderStatus.CREATED, dto.status());
            verify(outboxService).enqueueStockCarReservationRequested(any(main.domain.order.StockCarOrder.class));

            service.markStockCarReserved(dto.id());
            service.changeStockStatus(dto.id(), StockOrderStatus.AWAITING_PAYMENT);
            service.changeStockStatus(dto.id(), StockOrderStatus.PAID);

            assertEquals(StockOrderStatus.PAID, service.findStockById(dto.id()).status());
            verify(outboxService).enqueueStockCarWriteOffRequested(any(main.domain.order.StockCarOrder.class));
        }

        @Test
        void stockOrderCannotBePaidBeforeReservation() {
            User client = addClient();
            addManager();
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            StockOrderDto dto = service.createStockOrder(UUID.randomUUID());

            assertThrows(DomainValidationException.class,
                    () -> service.changeStockStatus(dto.id(), StockOrderStatus.PAID));
        }

        @Test
        void stockStorageReservationDecisionUpdatesState() {
            User client = addClient();
            addManager();
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            StockOrderDto dto = service.createStockOrder(UUID.randomUUID());

            service.markStockCarReserved(dto.id());
            assertEquals(StockOrderStatus.MANAGER_APPROVED, service.findStockById(dto.id()).status());
        }

        @Test
        void stockStorageReservationRejectionCancelsOrder() {
            User client = addClient();
            addManager();
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            StockOrderDto dto = service.createStockOrder(UUID.randomUUID());

            service.rejectStockCarReservation(dto.id());
            assertEquals(StockOrderStatus.CANCELLED, service.findStockById(dto.id()).status());
        }

        @Test
        void stockWriteOffDecisionUpdatesFinalState() {
            User client = addClient();
            addManager();
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            StockOrderDto dto = service.createStockOrder(UUID.randomUUID());
            service.markStockCarReserved(dto.id());
            service.changeStockStatus(dto.id(), StockOrderStatus.AWAITING_PAYMENT);
            service.changeStockStatus(dto.id(), StockOrderStatus.PAID);
            service.markStockCarWrittenOff(dto.id());

            assertEquals(StockOrderStatus.READY_FOR_HANDOVER, service.findStockById(dto.id()).status());
        }

        @Test
        void findAllOrders_returnsOnlyCurrentUsersOrdersForUser() {
            User firstClient = addClient();
            User secondClient = new User(UUID.randomUUID(), "Client 2", "c2@m.ru", Role.CLIENT);
            userRepo.save(secondClient);
            addManager();

            UUID firstCarId = UUID.randomUUID();
            UUID secondCarId = UUID.randomUUID();

            currentUserProvider.login(firstClient.id(), SecurityRoles.USER);
            service.createStockOrder(firstCarId);

            currentUserProvider.login(secondClient.id(), SecurityRoles.USER);
            service.createStockOrder(secondCarId);

            currentUserProvider.login(firstClient.id(), SecurityRoles.USER);
            assertEquals(1, service.findAllStock().size());

            currentUserProvider.login(UUID.randomUUID(), SecurityRoles.MANAGER);
            assertEquals(2, service.findAllStock().size());
        }

        @Test
        void customOrderCreationAndPaidFlow() {
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optionId, "Wheels", new Money(50_000), Set.of("320i"), partId, false),
                    18));
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            CustomOrderDto dto = service.createCustomOrder(modelId, Map.of(ConfigType.WHEELS, optionId));
            assertEquals(CustomOrderStatus.CREATED, dto.status());
            assertEquals(3_050_000, dto.totalPrice().rubles());
            verify(outboxService).enqueueOrderSentForApproval(any(CustomCarOrder.class), eq(List.of(new RequiredPartItem(partId, 1))));

            service.approveCustomOrder(dto.id());
            assertEquals(CustomOrderStatus.WAREHOUSE_APPROVED, service.findCustomById(dto.id()).status());
            service.changeCustomStatus(dto.id(), CustomOrderStatus.AWAITING_PAYMENT);
            service.changeCustomStatus(dto.id(), CustomOrderStatus.PAID);

            verify(outboxService).enqueueOrderExecutionRequested(any(CustomCarOrder.class));
        }

        @Test
        void customCancellationBeforePaidEnqueuesReservationRelease() {
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optionId, "Wheels", new Money(50_000), Set.of("320i"), partId, false),
                    18));
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            CustomOrderDto dto = service.createCustomOrder(modelId, Map.of(ConfigType.WHEELS, optionId));
            service.changeCustomStatus(dto.id(), CustomOrderStatus.CANCELLED);

            verify(outboxService).enqueueReservationReleaseRequested(any(CustomCarOrder.class));
        }

        @Test
        void customStorageDecisionUpdatesState() {
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optionId, "Wheels", new Money(50_000), Set.of("320i"), partId, false),
                    18));
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            CustomOrderDto dto = service.createCustomOrder(modelId, Map.of(ConfigType.WHEELS, optionId));
            service.approveCustomOrder(dto.id());
            service.changeCustomStatus(dto.id(), CustomOrderStatus.AWAITING_PAYMENT);
            service.changeCustomStatus(dto.id(), CustomOrderStatus.PAID);
            service.markCustomOrderAwaitingDelivery(dto.id());

            assertEquals(CustomOrderStatus.AWAITING_DELIVERY, service.findCustomById(dto.id()).status());
        }

        @Test
        void storageDecisionUpdatesFinalState() {
            User client = addClient();
            addManager();
            UUID carId = UUID.randomUUID();
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            StockOrderDto dto = service.createStockOrder(carId);
            service.markStockCarReserved(dto.id());
            service.changeStockStatus(dto.id(), StockOrderStatus.AWAITING_PAYMENT);
            service.changeStockStatus(dto.id(), StockOrderStatus.PAID);
            service.markOrderReadyForHandover(dto.id(), contracts.events.OrderType.STOCK);
            assertEquals(StockOrderStatus.READY_FOR_HANDOVER, service.findStockById(dto.id()).status());
        }
    }

    @Nested
    class TestDriveTests {
        InMemoryTestDriveRepository testDriveRepo;
        InMemoryUserRepository userRepo;
        StubCurrentUserProvider currentUserProvider;
        StorageReadClient storageReadClient;
        TestDriveService service;

        @BeforeEach
        void setup() {
            testDriveRepo = new InMemoryTestDriveRepository();
            userRepo = new InMemoryUserRepository();
            currentUserProvider = new StubCurrentUserProvider();
            storageReadClient = mock(StorageReadClient.class);
            service = new TestDriveService(testDriveRepo, userRepo, currentUserProvider, storageReadClient);
        }

        @Test
        void testDriveRequest_checksAvailabilityAndUserRole() {
            User client = new User(UUID.randomUUID(), "Client", "c@m.ru", Role.CLIENT);
            userRepo.save(client);
            UUID carId = UUID.randomUUID();
            when(storageReadClient.getCar(carId)).thenReturn(new StorageReadClient.StorageCarSnapshot(carId, true, true));
            currentUserProvider.login(client.id(), SecurityRoles.USER);

            TestDriveDto dto = service.createRequest(carId, LocalDateTime.now().plusDays(1));
            assertEquals(client.id(), dto.customerId());

            currentUserProvider.login(client.id(), SecurityRoles.USER);
            assertThrows(DomainValidationException.class, () -> service.createRequest(carId, LocalDateTime.now().minusDays(1)));
        }
    }

    @Nested
    class SystemAdminTests {
        SystemAdminService service;
        InMemoryCarModelRepository modelRepo;
        InMemoryUserRepository userRepo;
        InMemoryWheelOptionRepository wheelRepo;
        InMemoryTransmissionOptionRepository transmissionRepo;
        InMemorySteeringOptionRepository steeringRepo;
        InMemoryInteriorOptionRepository interiorRepo;
        InMemoryStockOrderRepository stockRepo;
        InMemoryCustomOrderRepository customRepo;
        InMemoryTestDriveRepository testDriveRepo;

        @BeforeEach
        void setup() {
            modelRepo = new InMemoryCarModelRepository();
            userRepo = new InMemoryUserRepository();
            wheelRepo = new InMemoryWheelOptionRepository();
            transmissionRepo = new InMemoryTransmissionOptionRepository();
            steeringRepo = new InMemorySteeringOptionRepository();
            interiorRepo = new InMemoryInteriorOptionRepository();
            stockRepo = new InMemoryStockOrderRepository();
            customRepo = new InMemoryCustomOrderRepository();
            testDriveRepo = new InMemoryTestDriveRepository();
            service = new SystemAdminService(modelRepo, userRepo, wheelRepo, transmissionRepo, steeringRepo, interiorRepo,
                    stockRepo, customRepo, testDriveRepo);
        }

        @Test
        void crudEntities() {
            CarModel model = new CarModel(UUID.randomUUID(), "BMW", "320i", new Money(3_000_000), PROPS, Set.of());
            service.saveCarModel(model);
            assertEquals(1, service.listCarModels().size());
            service.deleteCarModel(model.id());
            assertTrue(service.listCarModels().isEmpty());

            User user = new User(UUID.randomUUID(), "Test", "t@m.ru", Role.CLIENT);
            service.saveUser(user);
            assertEquals(1, service.listUsers().size());
            service.deleteUser(user.id());

            WheelOption option = new WheelOption(
                    new OptionSpec(UUID.randomUUID(), "Wheels", new Money(10_000), Set.of("320i"), UUID.randomUUID(), false),
                    18);
            service.saveWheelOption(option);
            assertEquals(1, service.listWheelOptions().size());
            service.deleteWheelOption(option.spec().id());
            assertTrue(service.listWheelOptions().isEmpty());
        }
    }

    static final class StubCurrentUserProvider implements CurrentUserProvider {
        private UUID currentUserId = UUID.randomUUID();
        private final Set<String> roles = new HashSet<>();

        void login(UUID userId, String... roles) {
            this.currentUserId = userId;
            this.roles.clear();
            this.roles.addAll(Set.of(roles));
        }

        @Override
        public UUID currentUserId() {
            return currentUserId;
        }

        @Override
        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        @Override
        public boolean hasAnyRole(String... roles) {
            for (String role : roles) {
                if (hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
    }
}

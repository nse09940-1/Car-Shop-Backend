package main;

import main.application.dto.*;
import main.application.service.*;
import main.domain.car.*;
import main.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import main.domain.Money;
import main.domain.configuration.ConfigType;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.WheelOption;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.exception.IncompatibleComponentException;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockOrderStatus;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.policy.DefaultCompatibilityPolicy;
import main.infrastructure.policy.DefaultOrderTransitionPolicy;
import main.infrastructure.policy.RandomManagerAssignmentPolicy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Nested
    class CatalogTests {
        InMemoryCarRepository carRepo;
        InMemoryCarModelRepository modelRepo;
        CarCatalogService service;

        @BeforeEach
        void setup() {
            carRepo = new InMemoryCarRepository();
            modelRepo = new InMemoryCarModelRepository();
            service = new CarCatalogService(carRepo, modelRepo);
        }

        @Test
        void findAvailableAndById() {
            // Arrange
            UUID m1 = UUID.randomUUID();
            UUID m2 = UUID.randomUUID();
            modelRepo.save(new CarModel(m1, "BMW", "320i", new Money(3000000), PROPS, Set.of()));
            modelRepo.save(new CarModel(m2, "Audi", "A4", new Money(2500000), PROPS, Set.of()));
            UUID carId = UUID.randomUUID();
            carRepo.save(new Car(carId, "V1", m1, "Red", new Money(3000000), true, false));
            carRepo.save(new Car(UUID.randomUUID(), "V2", m2, "Blue", new Money(2500000), true, false));
            carRepo.save(new Car(UUID.randomUUID(), "V3", m1, "Black", new Money(3100000), false, false));

            // Act
            int available = service.findAvailable(null).size();
            int bmwOnly = service.findAvailable(CarFilterRequest.byBrand("BMW")).size();
            String model = service.findById(carId).model();

            // Assert
            assertEquals(2, available);
            assertEquals(1, bmwOnly);
            assertEquals("320i", model);
            assertThrows(EntityNotFoundException.class, () -> service.findById(UUID.randomUUID()));
        }
    }

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
            // Arrange
            UUID modelId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(UUID.randomUUID(), "Base Wheels", new Money(0), Set.of("320i"), UUID.randomUUID(), true),
                    17));

            // Act
            CarConfigurationDto dto = service.getBaseConfiguration(modelId);

            // Assert
            assertEquals("Base Wheels", dto.selectedOptions().get(ConfigType.WHEELS));
        }

        @Test
        void buildConfiguration_valid() {
            // Arrange
            UUID modelId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "Sport Wheels", new Money(50000), Set.of("320i"), UUID.randomUUID(), false),
                    19));

            // Act
            ConfigurationPriceDto result = service.buildConfiguration(modelId,
                    Map.of(ConfigType.WHEELS, optId));

            // Assert
            assertEquals(3050000, result.totalPrice().rubles());
        }

        @Test
        void buildConfiguration_incompatibleOptionThrows() {
            // Arrange
            UUID modelId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "X5 Wheels", new Money(50000), Set.of("X5"), UUID.randomUUID(), false),
                    19));

            // Act Assert
            assertThrows(IncompatibleComponentException.class,
                    () -> service.buildConfiguration(modelId, Map.of(ConfigType.WHEELS, optId)));
        }
    }

    @Nested
    class OrderTests {
        InMemoryStockOrderRepository stockRepo;
        InMemoryCustomOrderRepository customRepo;
        InMemoryCarRepository carRepo;
        InMemoryPartRepository partRepo;
        InMemoryUserRepository userRepo;
        InMemoryCarModelRepository modelRepo;
        InMemoryWheelOptionRepository wheelRepo;
        InMemoryTransmissionOptionRepository transmissionRepo;
        InMemorySteeringOptionRepository steeringRepo;
        InMemoryInteriorOptionRepository interiorRepo;
        OrderService service;

        @BeforeEach
        void setup() {
            stockRepo = new InMemoryStockOrderRepository();
            customRepo = new InMemoryCustomOrderRepository();
            carRepo = new InMemoryCarRepository();
            partRepo = new InMemoryPartRepository();
            userRepo = new InMemoryUserRepository();
            modelRepo = new InMemoryCarModelRepository();
            wheelRepo = new InMemoryWheelOptionRepository();
            transmissionRepo = new InMemoryTransmissionOptionRepository();
            steeringRepo = new InMemorySteeringOptionRepository();
            interiorRepo = new InMemoryInteriorOptionRepository();

            var configurator = new ConfiguratorService(modelRepo, wheelRepo, transmissionRepo, steeringRepo, interiorRepo,
                    new DefaultCompatibilityPolicy());
            service = new OrderService(stockRepo, customRepo, carRepo, partRepo, userRepo,
                    new RandomManagerAssignmentPolicy(new Random(42)),
                    new DefaultOrderTransitionPolicy(),
                    configurator);
        }

        private User addClient() {
            User c = new User(UUID.randomUUID(), "Client", "c@m.ru", Role.CLIENT);
            userRepo.save(c);
            return c;
        }

        private User addManager() {
            User m = new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER);
            userRepo.save(m);
            return m;
        }

        @Test
        void stockOrderLifecycle() {
            // Arrange
            User client = addClient();
            addManager();
            Car car = new Car(UUID.randomUUID(), "V1", UUID.randomUUID(), "Red", new Money(2000000), true, false);
            carRepo.save(car);

            // Act
            StockOrderDto dto = service.createStockOrder(client.id(), car.id());
            StockOrderStatus createdStatus = dto.status();
            boolean carAvailableAfterCreate = carRepo.findById(car.id()).orElseThrow().available();

            Car unavailable = new Car(UUID.randomUUID(), "V2", UUID.randomUUID(), "B", new Money(1), false, false);
            carRepo.save(unavailable);

            // Assert
            assertEquals(StockOrderStatus.CREATED, createdStatus);
            assertFalse(carAvailableAfterCreate);
            assertEquals(StockOrderStatus.MANAGER_APPROVED,
                    service.changeStockStatus(dto.id(), StockOrderStatus.MANAGER_APPROVED).status());
            assertThrows(DomainValidationException.class,
                    () -> service.changeStockStatus(dto.id(), StockOrderStatus.COMPLETED));
            assertThrows(DomainValidationException.class,
                    () -> service.createStockOrder(client.id(), unavailable.id()));
        }

        @Test
        void customOrderCreation() {
            // Arrange
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            UUID partId = UUID.randomUUID();
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "Wheels", new Money(50000), Set.of("320i"), partId, false),
                    18));

            // Act
            CustomOrderDto dto = service.createCustomOrder(client.id(), modelId,
                    Map.of(ConfigType.WHEELS, optId));

            // Assert
            assertEquals(CustomOrderStatus.CREATED, dto.status());
            assertEquals(3050000, dto.totalPrice().rubles());
        }

        @Test
        void customOrderStatusTransitions_reserveAndReleasePart() {
            // Arrange
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            partRepo.save(new CarPart(partId, "Wheel kit", "WK-1", new Money(10000), Set.of("320i"), ConfigType.WHEELS, 3, 0));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "Wheels", new Money(50000), Set.of("320i"), partId, false),
                    18));

            CustomOrderDto created = service.createCustomOrder(client.id(), modelId, Map.of(ConfigType.WHEELS, optId));

            // Act
            service.changeCustomStatus(created.id(), CustomOrderStatus.WAREHOUSE_APPROVED);
            CarPart afterReserve = partRepo.findById(partId).orElseThrow();
            service.changeCustomStatus(created.id(), CustomOrderStatus.CANCELLED);
            CarPart afterCancel = partRepo.findById(partId).orElseThrow();

            // Assert
            assertEquals(1, afterReserve.reserved());
            assertEquals(3, afterReserve.inStock());
            assertEquals(0, afterCancel.reserved());
            assertEquals(3, afterCancel.inStock());
        }

        @Test
        void customOrderWarehouseApproval_failsWhenPartStockIsInsufficient() {
            // Arrange
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            partRepo.save(new CarPart(partId, "Wheel kit", "WK-2", new Money(10000), Set.of("320i"), ConfigType.WHEELS, 0, 0));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "Wheels", new Money(50000), Set.of("320i"), partId, false),
                    18));

            CustomOrderDto created = service.createCustomOrder(client.id(), modelId, Map.of(ConfigType.WHEELS, optId));

            // Act Assert
            assertThrows(DomainValidationException.class,
                    () -> service.changeCustomStatus(created.id(), CustomOrderStatus.WAREHOUSE_APPROVED));
        }

        @Test
        void customOrderWarehouseApproval_failsWhenPartTypeDoesNotMatchOptionType() {
            // Arrange
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            partRepo.save(new CarPart(partId, "Transmission kit", "TR-1", new Money(10000), Set.of("320i"), ConfigType.TRANSMISSION, 1, 0));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "Wheels", new Money(50000), Set.of("320i"), partId, false),
                    18));

            CustomOrderDto created = service.createCustomOrder(client.id(), modelId, Map.of(ConfigType.WHEELS, optId));

            // Act Assert
            assertThrows(DomainValidationException.class,
                    () -> service.changeCustomStatus(created.id(), CustomOrderStatus.WAREHOUSE_APPROVED));
        }

        @Test
        void customOrderCompletion_consumesReservedPart() {
            // Arrange
            User client = addClient();
            addManager();
            UUID modelId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            UUID optId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i",
                    new Money(3000000), PROPS, Set.of(ConfigType.WHEELS)));
            partRepo.save(new CarPart(partId, "Wheel kit", "WK-3", new Money(10000), Set.of("320i"), ConfigType.WHEELS, 2, 0));
            wheelRepo.save(new WheelOption(
                    new OptionSpec(optId, "Wheels", new Money(50000), Set.of("320i"), partId, false),
                    18));

            CustomOrderDto created = service.createCustomOrder(client.id(), modelId, Map.of(ConfigType.WHEELS, optId));

            // Act
            service.changeCustomStatus(created.id(), CustomOrderStatus.WAREHOUSE_APPROVED);
            service.changeCustomStatus(created.id(), CustomOrderStatus.AWAITING_PAYMENT);
            service.changeCustomStatus(created.id(), CustomOrderStatus.PAID);
            service.changeCustomStatus(created.id(), CustomOrderStatus.AWAITING_DELIVERY);
            service.changeCustomStatus(created.id(), CustomOrderStatus.READY_FOR_HANDOVER);
            service.changeCustomStatus(created.id(), CustomOrderStatus.COMPLETED);
            CarPart afterComplete = partRepo.findById(partId).orElseThrow();

            // Assert
            assertEquals(1, afterComplete.inStock());
            assertEquals(0, afterComplete.reserved());
        }
    }

    @Nested
    class TestDriveTests {
        InMemoryTestDriveRepository tdRepo;
        InMemoryCarRepository carRepo;
        InMemoryCarModelRepository modelRepo;
        InMemoryUserRepository userRepo;
        TestDriveService service;

        @BeforeEach
        void setup() {
            tdRepo = new InMemoryTestDriveRepository();
            carRepo = new InMemoryCarRepository();
            modelRepo = new InMemoryCarModelRepository();
            userRepo = new InMemoryUserRepository();
            service = new TestDriveService(tdRepo, carRepo, modelRepo, userRepo);
        }

        @Test
        void testDriveFullCycle() {
            // Arrange
            User client = new User(UUID.randomUUID(), "Client", "c@m.ru", Role.CLIENT);
            userRepo.save(client);
            UUID modelId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3000000), PROPS, Set.of()));
            Car car = new Car(UUID.randomUUID(), "V1", modelId, "Red", new Money(3000000), true, false);
            carRepo.save(car);

            // Act
            service.addCarToTestDriveList(car.id());
            boolean inTestDriveList = carRepo.findById(car.id()).orElseThrow().availableForTestDrive();
            int testDriveCarsCount = service.getTestDriveCars().size();
            TestDriveDto dto = service.createRequest(client.id(), car.id(), LocalDateTime.now().plusDays(1));
            User mgr = new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER);
            userRepo.save(mgr);
            service.removeCarFromTestDriveList(car.id());
            boolean inTestDriveListAfterRemove = carRepo.findById(car.id()).orElseThrow().availableForTestDrive();

            // Assert
            assertTrue(inTestDriveList);
            assertEquals(1, testDriveCarsCount);
            assertEquals(client.id(), dto.customerId());
            assertThrows(DomainValidationException.class,
                    () -> service.createRequest(mgr.id(), car.id(), LocalDateTime.now().plusDays(1)));
            assertFalse(inTestDriveListAfterRemove);
        }
    }

    @Nested
    class PartTests {
        InMemoryPartRepository partRepo;
        PartService service;

        @BeforeEach
        void setup() {
            partRepo = new InMemoryPartRepository();
            service = new PartService(partRepo);
        }

        @Test
        void partFullCrud() {
            // Arrange
            PartDto input = new PartDto(null, "Brake", "BBBB", new Money(5000), Set.of("320i"), ConfigType.WHEELS);

            // Act
            PartDto created = service.create(input);
            String createdName = service.findById(created.id()).name();
            String updatedName = service.update(created.id(),
                    new PartDto(created.id(), "Brake v2", "BBBB", new Money(6000), Set.of("320i"), ConfigType.WHEELS)).name();
            service.create(new PartDto(null, "F", "F1", new Money(1), Set.of(), ConfigType.STEERING));
            int allCount = service.findAll().size();
            service.delete(created.id());

            // Assert
            assertNotNull(created.id());
            assertEquals("Brake", createdName);
            assertEquals("Brake v2", updatedName);
            assertEquals(2, allCount);
            assertThrows(EntityNotFoundException.class, () -> service.findById(created.id()));
        }
    }

    @Nested
    class SystemAdminTests {
        SystemAdminService service;
        InMemoryCarModelRepository modelRepo;
        InMemoryCarRepository carRepo;
        InMemoryPartRepository partRepo;
        InMemoryUserRepository userRepo;
        InMemoryWheelOptionRepository wheelRepo;
        InMemoryTransmissionOptionRepository transmissionRepo;
        InMemorySteeringOptionRepository steeringRepo;
        InMemoryInteriorOptionRepository interiorRepo;
        InMemoryStockOrderRepository stockRepo;
        InMemoryCustomOrderRepository customRepo;
        InMemoryTestDriveRepository tdRepo;

        @BeforeEach
        void setup() {
            modelRepo = new InMemoryCarModelRepository();
            carRepo = new InMemoryCarRepository();
            partRepo = new InMemoryPartRepository();
            userRepo = new InMemoryUserRepository();
            wheelRepo = new InMemoryWheelOptionRepository();
            transmissionRepo = new InMemoryTransmissionOptionRepository();
            steeringRepo = new InMemorySteeringOptionRepository();
            interiorRepo = new InMemoryInteriorOptionRepository();
            stockRepo = new InMemoryStockOrderRepository();
            customRepo = new InMemoryCustomOrderRepository();
            tdRepo = new InMemoryTestDriveRepository();
            service = new SystemAdminService(modelRepo, carRepo, partRepo, userRepo,
                    wheelRepo, transmissionRepo, steeringRepo, interiorRepo,
                    stockRepo, customRepo, tdRepo);
        }

        @Test
        void crudEntities() {
            // Arrange
            CarModel m = new CarModel(UUID.randomUUID(), "BMW", "320i", new Money(3000000), PROPS, Set.of());

            // Act
            service.saveCarModel(m);
            assertEquals(1, service.listCarModels().size());
            service.deleteCarModel(m.id());
            assertTrue(service.listCarModels().isEmpty());

            User u = new User(UUID.randomUUID(), "Test", "t@m.ru", Role.CLIENT);
            service.saveUser(u);
            assertEquals(1, service.listUsers().size());
            service.deleteUser(u.id());

            UUID partId = UUID.randomUUID();
            service.savePart(new CarPart(partId, "Wheel kit", "WK-5", new Money(10000), Set.of("320i"), ConfigType.WHEELS));
            WheelOption opt = new WheelOption(
                    new OptionSpec(UUID.randomUUID(), "Wheels", new Money(10000), Set.of("320i"), partId, false),
                    18);
            service.saveWheelOption(opt);
            assertEquals(1, service.listWheelOptions().size());
            service.deleteWheelOption(opt.spec().id());
            assertTrue(service.listWheelOptions().isEmpty());
        }

        @Test
        void saveTransmissionOption_rejectsPartWithWrongType() {
            // Arrange
            UUID partId = UUID.randomUUID();
            service.savePart(new CarPart(partId, "Wheel kit", "WK-4", new Money(10000), Set.of("320i"), ConfigType.WHEELS));
            TransmissionOption transmission = new TransmissionOption(
                    new OptionSpec(UUID.randomUUID(), "MT 6", new Money(0), Set.of("320i"), partId, false),
                    TransmissionType.MANUAL);

            // Act Assert
            assertThrows(DomainValidationException.class, () -> service.saveTransmissionOption(transmission));
        }
    }
}

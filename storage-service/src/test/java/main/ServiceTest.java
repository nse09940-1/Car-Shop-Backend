package main;

import contracts.events.OrderSentForApprovalPayload;
import contracts.events.OrderType;
import contracts.events.RequiredPartItem;
import contracts.events.StockCarOperationPayload;
import main.application.dto.CarCardDto;
import main.application.dto.PartDto;
import main.application.service.AssemblyOrderService;
import main.application.service.CarCatalogService;
import main.application.service.PartService;
import main.application.service.SystemAdminService;
import main.domain.Money;
import main.domain.assembly.AssemblyOrderStatus;
import main.domain.car.Car;
import main.domain.car.CarModel;
import main.domain.car.CarPart;
import main.domain.car.CarProperty;
import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.Engine;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import main.domain.configuration.ConfigType;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.WheelOption;
import main.domain.exception.DomainValidationException;
import main.infrastructure.persistence.entity.AssemblyOrderJpaEntity;
import main.infrastructure.persistence.repository.AssemblyOrderJpaRepository;
import main.infrastructure.persistence.repository.AssemblyOrderRequiredPartJpaRepository;
import main.infrastructure.repository.InMemoryCarModelRepository;
import main.infrastructure.repository.InMemoryCarRepository;
import main.infrastructure.repository.InMemoryCustomOrderRepository;
import main.infrastructure.repository.InMemoryInteriorOptionRepository;
import main.infrastructure.repository.InMemoryPartRepository;
import main.infrastructure.repository.InMemorySteeringOptionRepository;
import main.infrastructure.repository.InMemoryStockOrderRepository;
import main.infrastructure.repository.InMemoryTestDriveRepository;
import main.infrastructure.repository.InMemoryTransmissionOptionRepository;
import main.infrastructure.repository.InMemoryUserRepository;
import main.infrastructure.repository.InMemoryWheelOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            UUID modelId = UUID.randomUUID();
            modelRepo.save(new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of()));
            UUID carId = UUID.randomUUID();
            carRepo.save(new Car(carId, "V1", modelId, "Red", new Money(3_000_000), true, false));
            carRepo.save(new Car(UUID.randomUUID(), "V2", modelId, "Blue", new Money(2_500_000), false, false));

            List<CarCardDto> available = service.findAvailable(null);
            assertEquals(1, available.size());
            assertEquals("320i", service.findById(carId).model());
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
            PartDto input = new PartDto(null, "Brake", "BBBB", new Money(5_000), Set.of("320i"), ConfigType.WHEELS);

            PartDto created = service.create(input);
            String createdName = service.findById(created.id()).name();
            String updatedName = service.update(created.id(),
                    new PartDto(created.id(), "Brake v2", "BBBB", new Money(6_000), Set.of("320i"), ConfigType.WHEELS)).name();
            service.create(new PartDto(null, "F", "F1", new Money(1), Set.of(), ConfigType.STEERING));
            int allCount = service.findAll().size();
            service.delete(created.id());

            assertNotNull(created.id());
            assertEquals("Brake", createdName);
            assertEquals("Brake v2", updatedName);
            assertEquals(2, allCount);
            assertThrows(main.domain.exception.EntityNotFoundException.class, () -> service.findById(created.id()));
        }
    }

    @Nested
    class AssemblyOrderTests {
        AssemblyOrderService service;
        AssemblyOrderJpaRepository assemblyRepo;
        AssemblyOrderRequiredPartJpaRepository requiredPartRepo;
        InMemoryCarRepository carRepo;
        InMemoryPartRepository partRepo;

        @BeforeEach
        void setup() {
            assemblyRepo = mock(AssemblyOrderJpaRepository.class);
            requiredPartRepo = mock(AssemblyOrderRequiredPartJpaRepository.class);
            carRepo = new InMemoryCarRepository();
            partRepo = new InMemoryPartRepository();
            service = new AssemblyOrderService(assemblyRepo, requiredPartRepo, carRepo, partRepo);

            when(assemblyRepo.save(any(AssemblyOrderJpaEntity.class))).thenAnswer(invocation -> {
                AssemblyOrderJpaEntity entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                if (entity.getStatus() == null) {
                    entity.setStatus(AssemblyOrderStatus.CREATED);
                }
                return entity;
            });
        }

        @Test
        void stockOrder_isApprovedWhenCarIsAvailable() {
            UUID orderId = UUID.randomUUID();
            UUID carId = UUID.randomUUID();
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.empty());
            carRepo.save(new Car(carId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), true, false));

            AssemblyOrderService.ProcessingResult result = service.processApprovalRequest(
                    orderId,
                    OrderType.STOCK,
                    new OrderSentForApprovalPayload(carId, null, List.of()));

            assertTrue(result.approved());
        }

        @Test
        void stockReservation_reservesAvailableCar() {
            UUID orderId = UUID.randomUUID();
            UUID carId = UUID.randomUUID();
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.empty());
            carRepo.save(new Car(carId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), true, false));

            AssemblyOrderService.ProcessingResult result = service.processStockReservationRequest(
                    orderId,
                    new StockCarOperationPayload(carId, null));

            assertTrue(result.approved());
            assertFalse(carRepo.findById(carId).orElseThrow().available());
        }

        @Test
        void stockReservation_rejectsUnavailableCar() {
            UUID orderId = UUID.randomUUID();
            UUID carId = UUID.randomUUID();
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.empty());
            carRepo.save(new Car(carId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), false, false));

            AssemblyOrderService.ProcessingResult result = service.processStockReservationRequest(
                    orderId,
                    new StockCarOperationPayload(carId, null));

            assertFalse(result.approved());
            assertEquals("Car is not available", result.reason());
        }

        @Test
        void stockWriteOff_assemblesReservedOrder() {
            UUID orderId = UUID.randomUUID();
            UUID assemblyOrderId = UUID.randomUUID();
            UUID carId = UUID.randomUUID();
            AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.STOCK);
            entity.setCarId(carId);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            entity.setRemoved(false);
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.of(entity));
            carRepo.save(new Car(carId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), false, false));

            AssemblyOrderService.ProcessingResult result = service.processStockWriteOffRequest(
                    orderId,
                    new StockCarOperationPayload(carId, null));

            assertTrue(result.approved());
            assertEquals(AssemblyOrderStatus.ASSEMBLED, entity.getStatus());
        }

        @Test
        void customOrder_isRejectedWhenPartStockIsInsufficient() {
            UUID orderId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.empty());
            partRepo.save(new CarPart(partId, "Wheel", "PART-1", new Money(10_000), Set.of("320i"), ConfigType.WHEELS, 1, 0));

            AssemblyOrderService.ProcessingResult result = service.processApprovalRequest(
                    orderId,
                    OrderType.CUSTOM,
                    new OrderSentForApprovalPayload(null, UUID.randomUUID(), List.of(new RequiredPartItem(partId, 2))));

            assertFalse(result.approved());
            assertEquals("Insufficient stock for part " + partId, result.reason());
        }

        @Test
        void customApproval_reservesPartsWithoutConsuming() {
            UUID orderId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.empty());
            partRepo.save(new CarPart(partId, "Wheel", "PART-1", new Money(10_000), Set.of("320i"), ConfigType.WHEELS, 5, 0));

            AssemblyOrderService.ProcessingResult result = service.processApprovalRequest(
                    orderId,
                    OrderType.CUSTOM,
                    new OrderSentForApprovalPayload(null, UUID.randomUUID(), List.of(new RequiredPartItem(partId, 2))));

            CarPart reserved = partRepo.findById(partId).orElseThrow();
            assertTrue(result.approved());
            assertEquals(5, reserved.inStock());
            assertEquals(2, reserved.reserved());
        }

        @Test
        void customExecution_consumesAlreadyReservedParts() {
            UUID orderId = UUID.randomUUID();
            UUID assemblyOrderId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.CUSTOM);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            entity.setRemoved(false);
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.of(entity));
            var requiredPart = new main.infrastructure.persistence.entity.AssemblyOrderRequiredPartJpaEntity();
            requiredPart.setAssemblyOrderId(assemblyOrderId);
            requiredPart.setPartId(partId);
            requiredPart.setQuantity(2);
            when(requiredPartRepo.findAllByAssemblyOrderId(assemblyOrderId)).thenReturn(List.of(requiredPart));
            partRepo.save(new CarPart(partId, "Wheel", "PART-1", new Money(10_000), Set.of("320i"), ConfigType.WHEELS, 5, 2));

            AssemblyOrderService.ProcessingResult result = service.processExecutionRequest(orderId, OrderType.CUSTOM);

            CarPart consumed = partRepo.findById(partId).orElseThrow();
            assertTrue(result.approved());
            assertEquals(3, consumed.inStock());
            assertEquals(0, consumed.reserved());
            assertEquals(AssemblyOrderStatus.IN_PROGRESS, entity.getStatus());
        }

        @Test
        void customRelease_releasesReservationAndIsIdempotent() {
            UUID orderId = UUID.randomUUID();
            UUID assemblyOrderId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();
            AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.CUSTOM);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            entity.setRemoved(false);
            when(assemblyRepo.findBySourceOrderIdAndRemovedFalse(orderId)).thenReturn(java.util.Optional.of(entity));
            var requiredPart = new main.infrastructure.persistence.entity.AssemblyOrderRequiredPartJpaEntity();
            requiredPart.setAssemblyOrderId(assemblyOrderId);
            requiredPart.setPartId(partId);
            requiredPart.setQuantity(2);
            when(requiredPartRepo.findAllByAssemblyOrderId(assemblyOrderId)).thenReturn(List.of(requiredPart));
            partRepo.save(new CarPart(partId, "Wheel", "PART-1", new Money(10_000), Set.of("320i"), ConfigType.WHEELS, 5, 2));

            service.processReservationRelease(orderId, OrderType.CUSTOM);
            service.processReservationRelease(orderId, OrderType.CUSTOM);

            CarPart released = partRepo.findById(partId).orElseThrow();
            assertEquals(5, released.inStock());
            assertEquals(0, released.reserved());
            assertEquals(AssemblyOrderStatus.RELEASED, entity.getStatus());
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
        InMemoryTestDriveRepository testDriveRepo;

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
            testDriveRepo = new InMemoryTestDriveRepository();
            service = new SystemAdminService(modelRepo, carRepo, partRepo, userRepo,
                    wheelRepo, transmissionRepo, steeringRepo, interiorRepo,
                    stockRepo, customRepo, testDriveRepo);
        }

        @Test
        void crudEntitiesAndOptionValidation() {
            UUID partId = UUID.randomUUID();
            service.savePart(new CarPart(partId, "Wheel kit", "WK-5", new Money(10_000), Set.of("320i"), ConfigType.WHEELS));
            WheelOption wheel = new WheelOption(
                    new OptionSpec(UUID.randomUUID(), "Wheels", new Money(10_000), Set.of("320i"), partId, false),
                    18);
            service.saveWheelOption(wheel);
            assertEquals(1, service.listWheelOptions().size());
            service.deleteWheelOption(wheel.spec().id());
            assertTrue(service.listWheelOptions().isEmpty());

            TransmissionOption transmission = new TransmissionOption(
                    new OptionSpec(UUID.randomUUID(), "AT", new Money(0), Set.of("320i"), partId, false),
                    TransmissionType.MANUAL);
            assertThrows(DomainValidationException.class, () -> service.saveTransmissionOption(transmission));
        }
    }
}

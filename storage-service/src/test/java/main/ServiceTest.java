package main;

import contracts.events.OrderSentForApprovalPayload;
import contracts.events.OrderType;
import contracts.events.RequiredPartItem;
import contracts.events.StockCarOperationPayload;
import main.application.dto.PartDto;
import main.application.port.repository.AssemblyOrderRepository;
import main.application.port.repository.AssemblyOrderRequiredPartRepository;
import main.application.service.AssemblyOrderService;
import main.application.service.PartService;
import main.domain.Money;
import main.domain.assembly.AssemblyOrder;
import main.domain.assembly.AssemblyOrderStatus;
import main.domain.assembly.AssemblyRequiredPart;
import main.domain.car.Car;
import main.domain.car.CarPart;
import main.domain.configuration.ConfigType;
import main.infrastructure.repository.InMemoryCarRepository;
import main.infrastructure.repository.InMemoryPartRepository;
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
        AssemblyOrderRepository assemblyRepo;
        AssemblyOrderRequiredPartRepository requiredPartRepo;
        InMemoryCarRepository carRepo;
        InMemoryPartRepository partRepo;

        @BeforeEach
        void setup() {
            assemblyRepo = mock(AssemblyOrderRepository.class);
            requiredPartRepo = mock(AssemblyOrderRequiredPartRepository.class);
            carRepo = new InMemoryCarRepository();
            partRepo = new InMemoryPartRepository();
            service = new AssemblyOrderService(assemblyRepo, requiredPartRepo, carRepo, partRepo);

            when(assemblyRepo.save(any(AssemblyOrder.class))).thenAnswer(invocation -> {
                AssemblyOrder entity = invocation.getArgument(0);
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
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.empty());
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
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.empty());
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
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.empty());
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
            AssemblyOrder entity = new AssemblyOrder();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.STOCK);
            entity.setCarId(carId);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.of(entity));
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
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.empty());
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
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.empty());
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
            AssemblyOrder entity = new AssemblyOrder();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.CUSTOM);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.of(entity));
            when(requiredPartRepo.findAllByAssemblyOrderId(assemblyOrderId))
                    .thenReturn(List.of(new AssemblyRequiredPart(partId, 2)));
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
            AssemblyOrder entity = new AssemblyOrder();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.CUSTOM);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.of(entity));
            when(requiredPartRepo.findAllByAssemblyOrderId(assemblyOrderId))
                    .thenReturn(List.of(new AssemblyRequiredPart(partId, 2)));
            partRepo.save(new CarPart(partId, "Wheel", "PART-1", new Money(10_000), Set.of("320i"), ConfigType.WHEELS, 5, 2));

            AssemblyOrderService.ProcessingResult first = service.processReservationRelease(orderId, OrderType.CUSTOM);
            AssemblyOrderService.ProcessingResult second = service.processReservationRelease(orderId, OrderType.CUSTOM);

            CarPart released = partRepo.findById(partId).orElseThrow();
            assertTrue(first.approved());
            assertTrue(second.approved());
            assertEquals(5, released.inStock());
            assertEquals(0, released.reserved());
            assertEquals(AssemblyOrderStatus.RELEASED, entity.getStatus());
        }

        @Test
        void customRelease_rejectsWhenReservationIsNotActive() {
            UUID orderId = UUID.randomUUID();
            AssemblyOrder entity = new AssemblyOrder();
            entity.setId(UUID.randomUUID());
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.CUSTOM);
            entity.setStatus(AssemblyOrderStatus.IN_PROGRESS);
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.of(entity));

            AssemblyOrderService.ProcessingResult result = service.processReservationRelease(orderId, OrderType.CUSTOM);

            assertFalse(result.approved());
            assertEquals("Reservation cannot be released from status IN_PROGRESS", result.reason());
        }

        @Test
        void stockRelease_returnsCarToAvailabilityAndIsIdempotent() {
            UUID orderId = UUID.randomUUID();
            UUID assemblyOrderId = UUID.randomUUID();
            UUID carId = UUID.randomUUID();
            AssemblyOrder entity = new AssemblyOrder();
            entity.setId(assemblyOrderId);
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.STOCK);
            entity.setCarId(carId);
            entity.setStatus(AssemblyOrderStatus.RESERVED);
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.of(entity));
            carRepo.save(new Car(carId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), false, false));

            AssemblyOrderService.ProcessingResult first = service.processReservationRelease(orderId, OrderType.STOCK);
            AssemblyOrderService.ProcessingResult second = service.processReservationRelease(orderId, OrderType.STOCK);

            assertTrue(first.approved());
            assertTrue(second.approved());
            assertTrue(carRepo.findById(carId).orElseThrow().available());
            assertEquals(AssemblyOrderStatus.RELEASED, entity.getStatus());
        }

        @Test
        void stockRelease_rejectsAfterWriteOff() {
            UUID orderId = UUID.randomUUID();
            AssemblyOrder entity = new AssemblyOrder();
            entity.setId(UUID.randomUUID());
            entity.setSourceOrderId(orderId);
            entity.setSourceOrderType(OrderType.STOCK);
            entity.setCarId(UUID.randomUUID());
            entity.setStatus(AssemblyOrderStatus.ASSEMBLED);
            when(assemblyRepo.findBySourceOrderId(orderId)).thenReturn(java.util.Optional.of(entity));

            AssemblyOrderService.ProcessingResult result = service.processReservationRelease(orderId, OrderType.STOCK);

            assertFalse(result.approved());
            assertEquals("Reservation cannot be released from status ASSEMBLED", result.reason());
        }
    }
}

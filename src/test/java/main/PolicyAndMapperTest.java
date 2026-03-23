package main;

import main.domain.car.*;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import org.junit.jupiter.api.Test;
import main.application.mapper.AppMapper;
import main.domain.Money;
import main.domain.TestDriveRequest;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.WheelOption;
import main.domain.exception.DomainValidationException;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.policy.DefaultCompatibilityPolicy;
import main.infrastructure.policy.DefaultOrderTransitionPolicy;
import main.infrastructure.policy.RandomManagerAssignmentPolicy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PolicyAndMapperTest {

    @Test
    void compatibilityPolicy_allCases() {
        // Arrange
        var policy = new DefaultCompatibilityPolicy();
        OptionSpec spec = new OptionSpec(UUID.randomUUID(), "19'' M-Sport",
                new Money(95000), Set.of("320i", "330i"), UUID.randomUUID(), false);
        WheelOption opt = new WheelOption(spec, 19);

        // Act
        boolean compatible = policy.isCompatible("320i", opt.spec());
        boolean incompatible = policy.isCompatible("X5", opt.spec());
        boolean nullCase = policy.isCompatible("320i", null);

        // Assert
        assertTrue(compatible);
        assertFalse(incompatible);
        assertFalse(nullCase);
    }

    @Test
    void orderTransitionPolicy_allCases() {
        // Arrange
        var policy = new DefaultOrderTransitionPolicy();

        // Act
        boolean stockCreatedToApproved = policy.canTransition(StockOrderStatus.CREATED, StockOrderStatus.MANAGER_APPROVED);
        boolean stockCreatedToCancelled = policy.canTransition(StockOrderStatus.CREATED, StockOrderStatus.CANCELLED);
        boolean stockCreatedToPaid = policy.canTransition(StockOrderStatus.CREATED, StockOrderStatus.PAID);
        boolean stockCompletedToCreated = policy.canTransition(StockOrderStatus.COMPLETED, StockOrderStatus.CREATED);
        boolean customCreatedToWarehouse = policy.canTransition(CustomOrderStatus.CREATED, CustomOrderStatus.WAREHOUSE_APPROVED);
        boolean customCreatedToCompleted = policy.canTransition(CustomOrderStatus.CREATED, CustomOrderStatus.COMPLETED);

        // Assert
        assertTrue(stockCreatedToApproved);
        assertTrue(stockCreatedToCancelled);
        assertFalse(stockCreatedToPaid);
        assertFalse(stockCompletedToCreated);
        assertTrue(customCreatedToWarehouse);
        assertFalse(customCreatedToCompleted);
    }

    @Test
    void managerAssignment_allCases() {
        // Arrange
        var policy = new RandomManagerAssignmentPolicy(new Random(42));
        User m = new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER);

        // Act
        User assigned = policy.assignManager(List.of(m));

        // Assert
        assertEquals(m, assigned);
        assertThrows(DomainValidationException.class, () -> policy.assignManager(List.of()));
        assertThrows(DomainValidationException.class, () -> policy.assignManager(null));
    }

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Test
    void mapper_allDtoConversions() {
        // Arrange
        UUID modelId = UUID.randomUUID();
        CarModel model = new CarModel(modelId, "BMW", "320i", new Money(3000000), PROPS, Set.of());
        Car car = new Car(UUID.randomUUID(), "VIN-1", modelId, "Red", new Money(3500000), true, false);
        StockCarOrder stockOrder = new StockCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), StockOrderStatus.CREATED);
        CustomCarOrder customOrder = new CustomCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
                new CarConfiguration(null, null, null, null), new Money(4000000), CustomOrderStatus.CREATED);
        TestDriveRequest req = new TestDriveRequest(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now().plusDays(2));
        var part = new CarPart(UUID.randomUUID(), "Brake", "BBBB", new Money(5000), Set.of("320i"), ConfigType.WHEELS);

        // Act
        String carBrand = AppMapper.toDto(car, model).brand();
        UUID stockDtoId = AppMapper.toDto(stockOrder).id();
        long customTotal = AppMapper.toDto(customOrder).totalPrice().rubles();
        UUID testDriveCustomerId = AppMapper.toDto(req).customerId();
        var back = AppMapper.toDomain(AppMapper.toDto(part));

        // Assert
        assertEquals("BMW", carBrand);
        assertEquals(stockOrder.id(), stockDtoId);
        assertEquals(4000000, customTotal);
        assertEquals(req.customerId(), testDriveCustomerId);
        assertEquals(part.id(), back.id());
    }
}

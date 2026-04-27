package main;

import main.application.mapper.AppMapper;
import main.domain.Money;
import main.domain.TestDriveRequest;
import main.domain.car.Car;
import main.domain.car.CarModel;
import main.domain.car.CarPart;
import main.domain.car.CarProperty;
import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.Engine;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.WheelOption;
import main.domain.exception.DomainValidationException;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.policy.DefaultCompatibilityPolicy;
import main.infrastructure.policy.DefaultOrderTransitionPolicy;
import main.infrastructure.policy.RandomManagerAssignmentPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyAndMapperTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Test
    void compatibilityPolicy_allCases() {
        var policy = new DefaultCompatibilityPolicy();
        OptionSpec spec = new OptionSpec(UUID.randomUUID(), "19'' M-Sport",
                new Money(95_000), Set.of("320i", "330i"), UUID.randomUUID(), false);
        WheelOption opt = new WheelOption(spec, 19);

        assertTrue(policy.isCompatible("320i", opt.spec()));
        assertFalse(policy.isCompatible("X5", opt.spec()));
        assertFalse(policy.isCompatible("320i", null));
    }

    @Test
    void orderTransitionPolicy_allCases() {
        var policy = new DefaultOrderTransitionPolicy();

        assertTrue(policy.canTransition(StockOrderStatus.CREATED, StockOrderStatus.MANAGER_APPROVED));
        assertTrue(policy.canTransition(StockOrderStatus.CREATED, StockOrderStatus.CANCELLED));
        assertFalse(policy.canTransition(StockOrderStatus.CREATED, StockOrderStatus.PAID));
        assertTrue(policy.canTransition(StockOrderStatus.MANAGER_APPROVED, StockOrderStatus.AWAITING_PAYMENT));
        assertTrue(policy.canTransition(StockOrderStatus.PAID, StockOrderStatus.READY_FOR_HANDOVER));
        assertFalse(policy.canTransition(StockOrderStatus.COMPLETED, StockOrderStatus.CREATED));
        assertTrue(policy.canTransition(CustomOrderStatus.CREATED, CustomOrderStatus.WAREHOUSE_APPROVED));
        assertFalse(policy.canTransition(CustomOrderStatus.CREATED, CustomOrderStatus.AWAITING_PAYMENT));
        assertTrue(policy.canTransition(CustomOrderStatus.PAID, CustomOrderStatus.AWAITING_DELIVERY));
    }

    @Test
    void managerAssignment_allCases() {
        var policy = new RandomManagerAssignmentPolicy(new Random(42));
        User manager = new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER);

        assertEquals(manager, policy.assignManager(List.of(manager)));
        assertThrows(DomainValidationException.class, () -> policy.assignManager(List.of()));
        assertThrows(DomainValidationException.class, () -> policy.assignManager(null));
    }

    @Test
    void mapper_allDtoConversions() {
        UUID modelId = UUID.randomUUID();
        CarModel model = new CarModel(modelId, "BMW", "320i", new Money(3_000_000), PROPS, Set.of());
        Car car = new Car(UUID.randomUUID(), "VIN-1", modelId, "Red", new Money(3_500_000), true, false);
        StockCarOrder stockOrder = new StockCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), StockOrderStatus.CREATED);
        CustomCarOrder customOrder = new CustomCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
                new CarConfiguration(null, null, null, null), new Money(4_000_000), CustomOrderStatus.CREATED);
        TestDriveRequest request = new TestDriveRequest(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now().plusDays(2));
        CarPart part = new CarPart(UUID.randomUUID(), "Brake", "BBBB", new Money(5_000), Set.of("320i"), ConfigType.WHEELS);

        assertEquals("BMW", AppMapper.toDto(car, model).brand());
        assertEquals(stockOrder.id(), AppMapper.toDto(stockOrder).id());
        assertEquals(4_000_000, AppMapper.toDto(customOrder).totalPrice().rubles());
        assertEquals(request.customerId(), AppMapper.toDto(request).customerId());
        assertEquals(part.id(), AppMapper.toDomain(AppMapper.toDto(part)).id());
    }
}

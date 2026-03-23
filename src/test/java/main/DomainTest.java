package main;

import main.domain.car.*;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.WheelOption;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import org.junit.jupiter.api.Test;
import main.domain.Money;
import main.domain.exception.DomainValidationException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Test
    void carAndRelatedEntities() {
        // Arrange
        Money a = new Money(300);
        Car car = new Car(UUID.randomUUID(), "VIN-1", UUID.randomUUID(), "Red", new Money(1000000), true, false);
        CarModel m = new CarModel(UUID.randomUUID(), "BMW", "320i",
                new Money(3000000), PROPS, Set.of(ConfigType.WHEELS));
        CarPart part = new CarPart(UUID.randomUUID(), "Brake", "BP-1", new Money(5000), Set.of("320i"), ConfigType.WHEELS);
        OptionSpec wheelSpec = new OptionSpec(
                UUID.randomUUID(), "M-Sport", new Money(95000), Set.of("320i"), UUID.randomUUID(), false);
        WheelOption wheelOption = new WheelOption(wheelSpec, 19);

        // Act
        Money sum = a.add(new Money(100));
        Money diff = a.subtract(new Money(100));
        Car unavailable = car.withAvailable(false);
        Car availableForTd = car.withAvailableForTestDrive(true);
        boolean compatible = wheelOption.spec().isCompatibleWith("320i");
        boolean incompatible = wheelOption.spec().isCompatibleWith("X5");

        // Assert
        assertEquals(400, sum.rubles());
        assertEquals(200, diff.rubles());
        assertEquals("Money[rubles=300]", a.toString());
        assertEquals("BMW", m.brand());
        assertEquals("Brake", part.name());
        assertFalse(unavailable.available());
        assertTrue(availableForTd.availableForTestDrive());
        assertTrue(compatible);
        assertFalse(incompatible);
        assertThrows(DomainValidationException.class,
                () -> new Engine(0, 2000));
    }

    @Test
    void configurationAndOrders() {
        // Arrange
        WheelOption wheel = new WheelOption(
                new OptionSpec(UUID.randomUUID(), "W", new Money(95000), Set.of("320i"), UUID.randomUUID(), true),
                17);
        Map<ConfigType, WheelOption> map = new EnumMap<>(ConfigType.class);
        map.put(ConfigType.WHEELS, wheel);
        CarConfiguration cfg = new CarConfiguration(map.get(ConfigType.WHEELS), null, null, null);

        // Act
        Money surcharge = cfg.totalSurcharge();
        StockCarOrder stock = new StockCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), StockOrderStatus.CREATED);
        stock.setStatus(StockOrderStatus.MANAGER_APPROVED);
        CustomCarOrder custom = new CustomCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
                new CarConfiguration(null, null, null, null), new Money(3000000), CustomOrderStatus.CREATED);

        // Assert
        assertEquals(95000, surcharge.rubles());
        assertDoesNotThrow(() -> cfg.validateRequiredNodes(Set.of(ConfigType.WHEELS)));
        assertThrows(DomainValidationException.class,
                () -> cfg.validateRequiredNodes(Set.of(ConfigType.WHEELS, ConfigType.STEERING)));
        assertEquals(StockOrderStatus.MANAGER_APPROVED, stock.status());
        assertEquals(3000000, custom.totalPrice().rubles());
    }
}

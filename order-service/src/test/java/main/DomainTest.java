package main;

import main.domain.Money;
import main.domain.TestDriveRequest;
import main.domain.car.CarModel;
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
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Test
    void moneyConfiguratorAndOrdersWork() {
        Money money = new Money(300);
        CarModel model = new CarModel(UUID.randomUUID(), "BMW", "320i",
                new Money(3_000_000), PROPS, Set.of(ConfigType.WHEELS));
        WheelOption wheelOption = new WheelOption(
                new OptionSpec(UUID.randomUUID(), "M-Sport", new Money(95_000), Set.of("320i"), UUID.randomUUID(), false),
                19);
        CarConfiguration configuration = new CarConfiguration(wheelOption, null, null, null);
        StockCarOrder stockOrder = new StockCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), StockOrderStatus.CREATED);
        CustomCarOrder customOrder = new CustomCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), model.id(), configuration, new Money(3_095_000), CustomOrderStatus.CREATED);
        TestDriveRequest request = new TestDriveRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1));

        assertEquals(400, money.add(new Money(100)).rubles());
        assertEquals(200, money.subtract(new Money(100)).rubles());
        assertTrue(wheelOption.spec().isCompatibleWith("320i"));
        assertEquals(95_000, configuration.totalSurcharge().rubles());
        assertDoesNotThrow(() -> configuration.validateRequiredNodes(Set.of(ConfigType.WHEELS)));
        stockOrder.setStatus(StockOrderStatus.MANAGER_APPROVED);
        assertEquals(StockOrderStatus.MANAGER_APPROVED, stockOrder.status());
        assertEquals(CustomOrderStatus.CREATED, customOrder.status());
        assertEquals(request.id(), request.id());
        assertThrows(DomainValidationException.class, () -> new Engine(0, 2000));
    }
}

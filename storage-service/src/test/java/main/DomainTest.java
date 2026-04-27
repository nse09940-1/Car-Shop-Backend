package main;

import main.domain.Money;
import main.domain.assembly.AssemblyOrderStatus;
import main.domain.car.Car;
import main.domain.car.CarPart;
import main.domain.configuration.ConfigType;
import main.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {

    @Test
    void carAndPartInventoryRulesWork() {
        Car car = new Car(UUID.randomUUID(), "VIN-1", UUID.randomUUID(), "Red", new Money(1_000_000), true, false);
        CarPart part = new CarPart(UUID.randomUUID(), "Wheel", "PART-1", new Money(10_000), Set.of("320i"), ConfigType.WHEELS, 3, 0);

        assertFalse(car.withAvailable(false).available());
        assertTrue(car.withAvailableForTestDrive(true).availableForTestDrive());
        assertEquals(2, part.reserve(1).availableToReserve());
        assertEquals(2, part.reserve(1).consume(1).inStock());
        assertEquals(AssemblyOrderStatus.ASSEMBLED, AssemblyOrderStatus.valueOf("ASSEMBLED"));
        assertThrows(DomainValidationException.class, () -> part.consume(1));
    }
}

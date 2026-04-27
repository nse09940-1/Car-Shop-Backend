package main;

import main.application.mapper.AppMapper;
import main.domain.Money;
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
import main.domain.configuration.WheelOption;
import main.infrastructure.policy.DefaultCompatibilityPolicy;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyAndMapperTest {

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
    void mapper_allDtoConversions() {
        UUID modelId = UUID.randomUUID();
        CarModel model = new CarModel(modelId, "BMW", "320i", new Money(3_000_000),
                new CarProperty(CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000), TransmissionType.AUTOMATIC, DriveType.FRONT),
                Set.of());
        Car car = new Car(UUID.randomUUID(), "VIN-1", modelId, "Red", new Money(3_500_000), true, false);
        CarPart part = new CarPart(UUID.randomUUID(), "Brake", "BBBB", new Money(5_000), Set.of("320i"), ConfigType.WHEELS);

        assertEquals("BMW", AppMapper.toDto(car, model).brand());
        assertEquals(part.id(), AppMapper.toDomain(AppMapper.toDto(part)).id());
    }
}

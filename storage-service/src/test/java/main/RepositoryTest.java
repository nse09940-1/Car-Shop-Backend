package main;

import main.domain.Money;
import main.domain.car.Car;
import main.domain.car.CarPart;
import main.domain.configuration.ConfigType;
import main.infrastructure.repository.InMemoryCarRepository;
import main.infrastructure.repository.InMemoryPartRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {

    @Test
    void simpleRepos_saveFindDelete() {
        var carRepo = new InMemoryCarRepository();
        Car car = new Car(UUID.randomUUID(), "VIN-001", UUID.randomUUID(), "Red", new Money(2_000_000), true, false);
        var partRepo = new InMemoryPartRepository();
        CarPart part = new CarPart(UUID.randomUUID(), "Brake", "BBBB", new Money(5_000), Set.of("320i"), ConfigType.WHEELS);

        carRepo.save(car);
        assertTrue(carRepo.findByVin("VIN-001").isPresent());
        carRepo.deleteById(car.id());
        assertTrue(carRepo.findAll().isEmpty());

        partRepo.save(part);
        assertEquals("Brake", partRepo.findById(part.id()).orElseThrow().name());
        partRepo.deleteById(part.id());
        assertTrue(partRepo.findAll().isEmpty());
    }
}

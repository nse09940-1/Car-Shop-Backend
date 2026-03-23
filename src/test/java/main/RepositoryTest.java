package main;

import main.domain.car.*;
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.infrastructure.repository.*;
import org.junit.jupiter.api.Test;
import main.domain.Money;
import main.domain.TestDriveRequest;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.WheelOption;
import main.domain.user.Role;
import main.domain.user.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Test
    void simpleRepos_saveFindDelete() {
        // Arrange
        var modelRepo = new InMemoryCarModelRepository();
        CarModel m = new CarModel(UUID.randomUUID(), "BMW", "320i", new Money(3000000), PROPS, Set.of());
        var carRepo = new InMemoryCarRepository();
        Car car = new Car(UUID.randomUUID(), "VIN-001", UUID.randomUUID(), "Red", new Money(2000000), true, false);
        var partRepo = new InMemoryPartRepository();
        CarPart part = new CarPart(UUID.randomUUID(), "Brake", "BBBB", new Money(5000), Set.of("320i"), ConfigType.WHEELS);
        var userRepo = new InMemoryUserRepository();

        // Act
        modelRepo.save(m);
        boolean modelFoundBeforeDelete = modelRepo.findById(m.id()).isPresent();
        modelRepo.deleteById(m.id());

        carRepo.save(car);
        boolean vinFound = carRepo.findByVin("VIN-001").isPresent();
        boolean vinMissing = carRepo.findByVin("MISSING").isEmpty();
        carRepo.deleteById(car.id());

        partRepo.save(part);
        String partName = partRepo.findById(part.id()).orElseThrow().name();
        partRepo.deleteById(part.id());

        userRepo.save(new User(UUID.randomUUID(), "Client", "c@m.ru", Role.CLIENT));
        userRepo.save(new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER));
        int managersCount = userRepo.findByRole(Role.MANAGER).size();

        // Assert
        assertTrue(modelFoundBeforeDelete);
        assertTrue(modelRepo.findAll().isEmpty());
        assertTrue(vinFound);
        assertTrue(vinMissing);
        assertTrue(carRepo.findAll().isEmpty());
        assertEquals("Brake", partName);
        assertTrue(partRepo.findAll().isEmpty());
        assertEquals(1, managersCount);
    }

    @Test
    void optionRepos_findByModelAndBaseAndDelete() {
        // Arrange
        var wheelRepo = new InMemoryWheelOptionRepository();
        WheelOption baseWheel = new WheelOption(
                new OptionSpec(UUID.randomUUID(), "Base Wheels", new Money(0), Set.of("320i"), UUID.randomUUID(), true),
                17);
        WheelOption premiumWheel = new WheelOption(
                new OptionSpec(UUID.randomUUID(), "Premium Wheels", new Money(50000), Set.of("320i"), UUID.randomUUID(), false),
                19);

        // Act
        wheelRepo.save(baseWheel);
        wheelRepo.save(premiumWheel);
        int byModelCount = wheelRepo.findByModelCode("320i").size();
        int byOtherNodeTypeCount = 0;
        boolean basePresentForModel = wheelRepo.findBaseByModelCode("320i").isPresent();
        boolean basePresentForX5 = wheelRepo.findBaseByModelCode("X5").isPresent();
        wheelRepo.deleteById(baseWheel.spec().id());
        int allAfterDelete = wheelRepo.findAll().size();

        // Assert
        assertEquals(2, byModelCount);
        assertEquals(0, byOtherNodeTypeCount);
        assertTrue(basePresentForModel);
        assertFalse(basePresentForX5);
        assertEquals(1, allAfterDelete);
    }

    @Test
    void orderAndTestDriveRepos_saveFindDelete() {
        // Arrange
        var stockRepo = new InMemoryStockOrderRepository();
        StockCarOrder stockOrder = new StockCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), StockOrderStatus.CREATED);
        var customRepo = new InMemoryCustomOrderRepository();
        CustomCarOrder customOrder = new CustomCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
                new CarConfiguration(null, null, null, null), new Money(3000000), CustomOrderStatus.CREATED);
        var tdRepo = new InMemoryTestDriveRepository();
        TestDriveRequest req = new TestDriveRequest(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now().plusDays(1));

        // Act
        stockRepo.save(stockOrder);
        boolean stockPresent = stockRepo.findById(stockOrder.id()).isPresent();
        stockRepo.deleteById(stockOrder.id());

        customRepo.save(customOrder);
        boolean customPresent = customRepo.findById(customOrder.id()).isPresent();
        customRepo.deleteById(customOrder.id());

        tdRepo.save(req);
        boolean tdPresent = tdRepo.findById(req.id()).isPresent();
        tdRepo.deleteById(req.id());

        // Assert
        assertTrue(stockPresent);
        assertTrue(stockRepo.findAll().isEmpty());
        assertTrue(customPresent);
        assertTrue(customRepo.findAll().isEmpty());
        assertTrue(tdPresent);
        assertTrue(tdRepo.findAll().isEmpty());
    }
}

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
import main.domain.order.CustomCarOrder;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockCarOrder;
import main.domain.order.StockOrderStatus;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.repository.InMemoryCarModelRepository;
import main.infrastructure.repository.InMemoryCustomOrderRepository;
import main.infrastructure.repository.InMemoryStockOrderRepository;
import main.infrastructure.repository.InMemoryTestDriveRepository;
import main.infrastructure.repository.InMemoryUserRepository;
import main.infrastructure.repository.InMemoryWheelOptionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {

    static final CarProperty PROPS = new CarProperty(
            CarType.SEDAN, FuelType.PETROL, new Engine(150, 2000),
            TransmissionType.AUTOMATIC, DriveType.FRONT);

    @Test
    void simpleRepos_saveFindDelete() {
        var modelRepo = new InMemoryCarModelRepository();
        CarModel model = new CarModel(UUID.randomUUID(), "BMW", "320i", new Money(3_000_000), PROPS, Set.of());
        var userRepo = new InMemoryUserRepository();

        modelRepo.save(model);
        assertTrue(modelRepo.findById(model.id()).isPresent());
        modelRepo.deleteById(model.id());
        assertTrue(modelRepo.findAll().isEmpty());

        userRepo.save(new User(UUID.randomUUID(), "Client", "c@m.ru", Role.CLIENT));
        userRepo.save(new User(UUID.randomUUID(), "Mgr", "m@m.ru", Role.MANAGER));
        assertEquals(1, userRepo.findByRole(Role.MANAGER).size());
    }

    @Test
    void optionRepo_findByModelAndBaseAndDelete() {
        var wheelRepo = new InMemoryWheelOptionRepository();
        WheelOption baseWheel = new WheelOption(
                new OptionSpec(UUID.randomUUID(), "Base Wheels", new Money(0), Set.of("320i"), UUID.randomUUID(), true),
                17);
        WheelOption premiumWheel = new WheelOption(
                new OptionSpec(UUID.randomUUID(), "Premium Wheels", new Money(50_000), Set.of("320i"), UUID.randomUUID(), false),
                19);

        wheelRepo.save(baseWheel);
        wheelRepo.save(premiumWheel);

        assertEquals(2, wheelRepo.findByModelCode("320i").size());
        assertTrue(wheelRepo.findBaseByModelCode("320i").isPresent());
        assertFalse(wheelRepo.findBaseByModelCode("X5").isPresent());
        wheelRepo.deleteById(baseWheel.spec().id());
        assertEquals(1, wheelRepo.findAll().size());
    }

    @Test
    void orderAndTestDriveRepos_saveFindDelete() {
        var stockRepo = new InMemoryStockOrderRepository();
        var customRepo = new InMemoryCustomOrderRepository();
        var testDriveRepo = new InMemoryTestDriveRepository();

        StockCarOrder stockOrder = new StockCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(), StockOrderStatus.CREATED);
        CustomCarOrder customOrder = new CustomCarOrder(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
                new CarConfiguration(null, null, null, null), new Money(3_000_000), CustomOrderStatus.CREATED);
        TestDriveRequest request = new TestDriveRequest(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.now().plusDays(1));

        stockRepo.save(stockOrder);
        customRepo.save(customOrder);
        testDriveRepo.save(request);

        assertTrue(stockRepo.findById(stockOrder.id()).isPresent());
        assertTrue(customRepo.findById(customOrder.id()).isPresent());
        assertTrue(testDriveRepo.findById(request.id()).isPresent());

        stockRepo.deleteById(stockOrder.id());
        customRepo.deleteById(customOrder.id());
        testDriveRepo.deleteById(request.id());

        assertTrue(stockRepo.findAll().isEmpty());
        assertTrue(customRepo.findAll().isEmpty());
        assertTrue(testDriveRepo.findAll().isEmpty());
    }
}

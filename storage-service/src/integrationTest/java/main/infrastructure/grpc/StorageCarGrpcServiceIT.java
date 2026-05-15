package main.infrastructure.grpc;

import contracts.grpc.GetAvailableCarRequest;
import contracts.grpc.GetCarStateRequest;
import contracts.grpc.ListAvailableCarsRequest;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import main.application.port.repository.CarRepository;
import main.application.service.StorageCarQueryService;
import main.domain.Money;
import main.domain.car.Car;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageCarGrpcServiceIT {
    private io.grpc.Server server;
    private io.grpc.ManagedChannel channel;
    private StorageCarServiceGrpc.StorageCarServiceBlockingStub stub;
    private TestCarRepository carRepository;

    @BeforeEach
    void setup() throws Exception {
        carRepository = new TestCarRepository();
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new StorageCarGrpcService(new StorageCarQueryService(carRepository)))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = StorageCarServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void listAvailableCars_returnsOnlyAvailableCars() {
        UUID availableId = UUID.randomUUID();
        carRepository.save(new Car(availableId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), true, true));
        carRepository.save(new Car(UUID.randomUUID(), "VIN-2", UUID.randomUUID(), "White", new Money(2_000_000), false, false));

        var response = stub.listAvailableCars(ListAvailableCarsRequest.newBuilder().build());

        assertEquals(1, response.getCarsCount());
        assertEquals(availableId.toString(), response.getCars(0).getId());
    }

    @Test
    void listAvailableCars_returnsEmptyResult() {
        var response = stub.listAvailableCars(ListAvailableCarsRequest.newBuilder().build());

        assertEquals(0, response.getCarsCount());
    }

    @Test
    void getAvailableCar_returnsCarOrNotFound() {
        UUID availableId = UUID.randomUUID();
        UUID unavailableId = UUID.randomUUID();
        carRepository.save(new Car(availableId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), true, true));
        carRepository.save(new Car(unavailableId, "VIN-2", UUID.randomUUID(), "White", new Money(2_000_000), false, false));

        var found = stub.getAvailableCar(GetAvailableCarRequest.newBuilder().setId(availableId.toString()).build());
        StatusRuntimeException missing = assertThrows(StatusRuntimeException.class,
                () -> stub.getAvailableCar(GetAvailableCarRequest.newBuilder().setId(UUID.randomUUID().toString()).build()));
        StatusRuntimeException unavailable = assertThrows(StatusRuntimeException.class,
                () -> stub.getAvailableCar(GetAvailableCarRequest.newBuilder().setId(unavailableId.toString()).build()));

        assertEquals(availableId.toString(), found.getId());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.NOT_FOUND, unavailable.getStatus().getCode());
    }

    @Test
    void getCarState_returnsAnyCarById() {
        UUID availableId = UUID.randomUUID();
        UUID unavailableId = UUID.randomUUID();
        carRepository.save(new Car(availableId, "VIN-1", UUID.randomUUID(), "Black", new Money(1_000_000), true, true));
        carRepository.save(new Car(unavailableId, "VIN-2", UUID.randomUUID(), "White", new Money(2_000_000), false, false));

        var unavailable = stub.getCarState(GetCarStateRequest.newBuilder().setId(unavailableId.toString()).build());
        StatusRuntimeException missing = assertThrows(StatusRuntimeException.class,
                () -> stub.getCarState(GetCarStateRequest.newBuilder().setId(UUID.randomUUID().toString()).build()));
        StatusRuntimeException invalidId = assertThrows(StatusRuntimeException.class,
                () -> stub.getCarState(GetCarStateRequest.newBuilder().setId("bad-id").build()));

        assertEquals(unavailableId.toString(), unavailable.getId());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, invalidId.getStatus().getCode());
    }

    private static class TestCarRepository implements CarRepository {
        private final Map<UUID, Car> storage = new HashMap<>();

        @Override
        public void save(Car car) {
            storage.put(car.id(), car);
        }

        @Override
        public Optional<Car> findById(UUID id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Car> findByVin(String vin) {
            return storage.values().stream().filter(car -> car.vin().equals(vin)).findFirst();
        }

        @Override
        public List<Car> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<Car> findAllAvailable() {
            return storage.values().stream().filter(Car::available).toList();
        }

        @Override
        public Optional<Car> findAvailableById(UUID id) {
            return findById(id).filter(Car::available);
        }

        @Override
        public void deleteById(UUID id) {
            storage.remove(id);
        }
    }
}

package main.infrastructure.grpc;

import contracts.grpc.GetAvailableCarRequest;
import contracts.grpc.GetCarStateRequest;
import contracts.grpc.ListAvailableCarsRequest;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import main.Application;
import main.infrastructure.persistence.entity.CarJpaEntity;
import main.infrastructure.persistence.repository.CarJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class)
@Testcontainers(disabledWithoutDocker = true)
class StorageCarGrpcFullStackIT {
    private static final UUID SEED_MODEL_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/test-issuer");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/test-jwks");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("app.kafka.consumer-group", () -> "storage-service-grpc-full-stack-it");
        registry.add("app.grpc.port", () -> "0");
    }

    @Autowired
    private StorageGrpcServer grpcServer;

    @Autowired
    private CarJpaRepository carJpaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private ManagedChannel channel;

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void grpcReadsAvailableCarsFromPostgresThroughSpringApplication() {
        UUID availableId = UUID.randomUUID();
        UUID unavailableId = UUID.randomUUID();
        UUID removedId = UUID.randomUUID();
        carJpaRepository.save(car(availableId, true, false));
        carJpaRepository.save(car(unavailableId, false, false));
        carJpaRepository.save(car(removedId, true, true));

        StorageCarServiceGrpc.StorageCarServiceBlockingStub stub = grpcStub();

        var list = stub.listAvailableCars(ListAvailableCarsRequest.newBuilder().build());
        var listedIds = list.getCarsList().stream().map(car -> UUID.fromString(car.getId())).toList();
        var found = stub.getAvailableCar(GetAvailableCarRequest.newBuilder()
                .setId(availableId.toString())
                .build());
        var state = stub.getCarState(GetCarStateRequest.newBuilder()
                .setId(unavailableId.toString())
                .build());
        StatusRuntimeException unavailable = assertThrows(StatusRuntimeException.class,
                () -> stub.getAvailableCar(GetAvailableCarRequest.newBuilder()
                        .setId(unavailableId.toString())
                        .build()));
        StatusRuntimeException removed = assertThrows(StatusRuntimeException.class,
                () -> stub.getAvailableCar(GetAvailableCarRequest.newBuilder()
                        .setId(removedId.toString())
                        .build()));

        assertTrue(listedIds.contains(availableId));
        assertFalse(listedIds.contains(unavailableId));
        assertFalse(listedIds.contains(removedId));
        assertEquals(availableId.toString(), found.getId());
        assertEquals(unavailableId.toString(), state.getId());
        assertFalse(state.getAvailable());
        assertEquals(Status.Code.NOT_FOUND, unavailable.getStatus().getCode());
        assertEquals(Status.Code.NOT_FOUND, removed.getStatus().getCode());
    }

    private StorageCarServiceGrpc.StorageCarServiceBlockingStub grpcStub() {
        channel = ManagedChannelBuilder.forAddress("localhost", grpcServer.getPort())
                .usePlaintext()
                .build();
        return StorageCarServiceGrpc.newBlockingStub(channel);
    }

    private CarJpaEntity car(UUID id, boolean available, boolean removed) {
        CarJpaEntity car = new CarJpaEntity();
        car.setId(id);
        car.setVin("VIN-GRPC-FULL-" + id);
        car.setCarModelId(SEED_MODEL_ID);
        car.setColor("Black");
        car.setPrice(1_000_000);
        car.setAvailable(available);
        car.setAvailableForTestDrive(true);
        car.setRemoved(removed);
        return car;
    }
}

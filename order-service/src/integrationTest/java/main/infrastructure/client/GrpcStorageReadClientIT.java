package main.infrastructure.client;

import contracts.grpc.CarStateResponse;
import contracts.grpc.GetCarStateRequest;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import main.domain.exception.EntityNotFoundException;
import main.domain.exception.StorageServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrpcStorageReadClientIT {
    private io.grpc.Server server;
    private io.grpc.ManagedChannel channel;

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void clientMapsSuccessfulResponse() throws Exception {
        UUID carId = UUID.randomUUID();
        GrpcStorageReadClient client = startClient(new FakeStorageCarService() {
            @Override
            public void getCarState(GetCarStateRequest request,
                                    StreamObserver<CarStateResponse> responseObserver) {
                responseObserver.onNext(CarStateResponse.newBuilder()
                        .setId(carId.toString())
                        .setAvailable(false)
                        .setAvailableForTestDrive(true)
                        .build());
                responseObserver.onCompleted();
            }
        }, 500);

        var car = client.getCar(carId);

        assertEquals(carId, car.id());
        assertFalse(car.available());
        assertTrue(car.availableForTestDrive());
    }

    @Test
    void clientMapsNotFound() throws Exception {
        GrpcStorageReadClient client = startClient(new FakeStorageCarService() {
            @Override
            public void getCarState(GetCarStateRequest request,
                                    StreamObserver<CarStateResponse> responseObserver) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            }
        }, 500);

        assertThrows(EntityNotFoundException.class, () -> client.getCar(UUID.randomUUID()));
    }

    @Test
    void clientMapsUnavailableAndTimeout() throws Exception {
        GrpcStorageReadClient unavailableClient = startClient(new FakeStorageCarService() {
            @Override
            public void getCarState(GetCarStateRequest request,
                                    StreamObserver<CarStateResponse> responseObserver) {
                responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
            }
        }, 500);

        assertThrows(StorageServiceUnavailableException.class, () -> unavailableClient.getCar(UUID.randomUUID()));

        GrpcStorageReadClient timeoutClient = startClient(new FakeStorageCarService() {
            @Override
            public void getCarState(GetCarStateRequest request,
                                    StreamObserver<CarStateResponse> responseObserver) {
                sleep(200);
                responseObserver.onNext(CarStateResponse.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setAvailable(true)
                        .setAvailableForTestDrive(true)
                        .build());
                responseObserver.onCompleted();
            }
        }, 50);

        assertThrows(StorageServiceUnavailableException.class, () -> timeoutClient.getCar(UUID.randomUUID()));
    }

    private GrpcStorageReadClient startClient(StorageCarServiceGrpc.StorageCarServiceImplBase service,
                                              long timeoutMs) throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        }
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .addService(service)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).build();
        return new GrpcStorageReadClient(channel, timeoutMs);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static class FakeStorageCarService extends StorageCarServiceGrpc.StorageCarServiceImplBase {
    }
}

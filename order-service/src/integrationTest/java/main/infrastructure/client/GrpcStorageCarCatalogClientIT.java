package main.infrastructure.client;

import contracts.grpc.AvailableCarResponse;
import contracts.grpc.GetAvailableCarRequest;
import contracts.grpc.ListAvailableCarsRequest;
import contracts.grpc.ListAvailableCarsResponse;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrpcStorageCarCatalogClientIT {
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
    void clientMapsSuccessfulResponses() throws Exception {
        UUID carId = UUID.randomUUID();
        UUID modelId = UUID.randomUUID();
        GrpcStorageCarCatalogClient client = startClient(new FakeStorageCarService() {
            @Override
            public void listAvailableCars(ListAvailableCarsRequest request,
                                          StreamObserver<ListAvailableCarsResponse> responseObserver) {
                responseObserver.onNext(ListAvailableCarsResponse.newBuilder()
                        .addCars(carResponse(carId, modelId))
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void getAvailableCar(GetAvailableCarRequest request,
                                        StreamObserver<AvailableCarResponse> responseObserver) {
                responseObserver.onNext(carResponse(carId, modelId));
                responseObserver.onCompleted();
            }
        }, 500);

        var list = client.findAvailableCars();
        var item = client.findAvailableCar(carId);

        assertEquals(1, list.size());
        assertEquals(carId, list.get(0).id());
        assertEquals(modelId, item.carModelId());
    }

    @Test
    void clientMapsNotFound() throws Exception {
        GrpcStorageCarCatalogClient client = startClient(new FakeStorageCarService() {
            @Override
            public void getAvailableCar(GetAvailableCarRequest request,
                                        StreamObserver<AvailableCarResponse> responseObserver) {
                responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
            }
        }, 500);

        assertThrows(EntityNotFoundException.class, () -> client.findAvailableCar(UUID.randomUUID()));
    }

    @Test
    void clientMapsUnavailable() throws Exception {
        GrpcStorageCarCatalogClient client = startClient(new FakeStorageCarService() {
            @Override
            public void listAvailableCars(ListAvailableCarsRequest request,
                                          StreamObserver<ListAvailableCarsResponse> responseObserver) {
                responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
            }
        }, 500);

        assertThrows(StorageServiceUnavailableException.class, client::findAvailableCars);
    }

    @Test
    void clientMapsTimeout() throws Exception {
        GrpcStorageCarCatalogClient client = startClient(new FakeStorageCarService() {
            @Override
            public void listAvailableCars(ListAvailableCarsRequest request,
                                          StreamObserver<ListAvailableCarsResponse> responseObserver) {
                sleep(200);
                responseObserver.onNext(ListAvailableCarsResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        }, 50);

        assertThrows(StorageServiceUnavailableException.class, client::findAvailableCars);
    }

    private GrpcStorageCarCatalogClient startClient(StorageCarServiceGrpc.StorageCarServiceImplBase service,
                                                   long timeoutMs) throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .addService(service)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).build();
        return new GrpcStorageCarCatalogClient(channel, timeoutMs);
    }

    private AvailableCarResponse carResponse(UUID carId, UUID modelId) {
        return AvailableCarResponse.newBuilder()
                .setId(carId.toString())
                .setVin("VIN-1")
                .setCarModelId(modelId.toString())
                .setColor("Black")
                .setPriceRubles(1_000_000)
                .setAvailableForTestDrive(true)
                .build();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static class FakeStorageCarService extends StorageCarServiceGrpc.StorageCarServiceImplBase {
        @Override
        public void listAvailableCars(ListAvailableCarsRequest request,
                                      StreamObserver<ListAvailableCarsResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.UNIMPLEMENTED));
        }

        @Override
        public void getAvailableCar(GetAvailableCarRequest request,
                                    StreamObserver<AvailableCarResponse> responseObserver) {
            responseObserver.onError(new StatusRuntimeException(Status.UNIMPLEMENTED));
        }
    }
}

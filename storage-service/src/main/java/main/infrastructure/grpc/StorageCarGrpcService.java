package main.infrastructure.grpc;

import contracts.grpc.AvailableCarResponse;
import contracts.grpc.CarStateResponse;
import contracts.grpc.GetAvailableCarRequest;
import contracts.grpc.GetCarStateRequest;
import contracts.grpc.ListAvailableCarsRequest;
import contracts.grpc.ListAvailableCarsResponse;
import contracts.grpc.StorageCarServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import main.application.service.StorageCarQueryService;
import main.domain.car.Car;
import main.domain.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StorageCarGrpcService extends StorageCarServiceGrpc.StorageCarServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(StorageCarGrpcService.class);

    private final StorageCarQueryService storageCarQueryService;

    public StorageCarGrpcService(StorageCarQueryService storageCarQueryService) {
        this.storageCarQueryService = storageCarQueryService;
    }

    @Override
    public void listAvailableCars(ListAvailableCarsRequest request,
                                  StreamObserver<ListAvailableCarsResponse> responseObserver) {
        long startedAt = System.nanoTime();
        try {
            var cars = storageCarQueryService.findAvailableCars();
            ListAvailableCarsResponse response = ListAvailableCarsResponse.newBuilder()
                    .addAllCars(cars.stream().map(this::toResponse).toList())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("gRPC listAvailableCars completed count={} durationMs={}", cars.size(), elapsedMs(startedAt));
        } catch (Exception ex) {
            log.warn("gRPC listAvailableCars failed durationMs={}", elapsedMs(startedAt), ex);
            responseObserver.onError(Status.INTERNAL.withDescription("Cannot load available cars").asRuntimeException());
        }
    }

    @Override
    public void getAvailableCar(GetAvailableCarRequest request,
                                StreamObserver<AvailableCarResponse> responseObserver) {
        long startedAt = System.nanoTime();
        try {
            UUID id = UUID.fromString(request.getId());
            Car car = storageCarQueryService.findAvailableCar(id);
            responseObserver.onNext(toResponse(car));
            responseObserver.onCompleted();
            log.info("gRPC getAvailableCar completed carId={} durationMs={}", id, elapsedMs(startedAt));
        } catch (IllegalArgumentException ex) {
            log.warn("gRPC getAvailableCar invalid id={} durationMs={}", request.getId(), elapsedMs(startedAt));
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid car id").asRuntimeException());
        } catch (EntityNotFoundException ex) {
            log.info("gRPC getAvailableCar not found carId={} durationMs={}", request.getId(), elapsedMs(startedAt));
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.warn("gRPC getAvailableCar failed carId={} durationMs={}", request.getId(), elapsedMs(startedAt), ex);
            responseObserver.onError(Status.INTERNAL.withDescription("Cannot load available car").asRuntimeException());
        }
    }

    @Override
    public void getCarState(GetCarStateRequest request,
                            StreamObserver<CarStateResponse> responseObserver) {
        long startedAt = System.nanoTime();
        try {
            UUID id = UUID.fromString(request.getId());
            Car car = storageCarQueryService.findCarById(id);
            responseObserver.onNext(toStateResponse(car));
            responseObserver.onCompleted();
            log.info("gRPC getCarState completed carId={} durationMs={}", id, elapsedMs(startedAt));
        } catch (IllegalArgumentException ex) {
            log.warn("gRPC getCarState invalid id={} durationMs={}", request.getId(), elapsedMs(startedAt));
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid car id").asRuntimeException());
        } catch (EntityNotFoundException ex) {
            log.info("gRPC getCarState not found carId={} durationMs={}", request.getId(), elapsedMs(startedAt));
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.warn("gRPC getCarState failed carId={} durationMs={}", request.getId(), elapsedMs(startedAt), ex);
            responseObserver.onError(Status.INTERNAL.withDescription("Cannot load car state").asRuntimeException());
        }
    }

    private AvailableCarResponse toResponse(Car car) {
        return AvailableCarResponse.newBuilder()
                .setId(car.id().toString())
                .setVin(car.vin())
                .setCarModelId(car.carModelId().toString())
                .setColor(car.color())
                .setPriceRubles(car.price().rubles())
                .setAvailableForTestDrive(car.availableForTestDrive())
                .build();
    }

    private CarStateResponse toStateResponse(Car car) {
        return CarStateResponse.newBuilder()
                .setId(car.id().toString())
                .setAvailable(car.available())
                .setAvailableForTestDrive(car.availableForTestDrive())
                .build();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

package main.application.port.client;

import java.util.UUID;

public interface StorageReadClient {
    StorageCarSnapshot getCar(UUID carId);

    default void requireAvailableCar(UUID carId) {
        StorageCarSnapshot car = getCar(carId);
        if (!car.available()) {
            throw new main.domain.exception.DomainValidationException("Car is not available");
        }
    }

    record StorageCarSnapshot(
            UUID id,
            boolean available,
            boolean availableForTestDrive) {
    }
}

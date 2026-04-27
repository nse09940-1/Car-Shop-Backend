package main.domain.car;

import main.domain.Money;

import java.util.Objects;
import java.util.UUID;

public record Car(
        UUID id,
        String vin,
        UUID carModelId,
        String color,
        Money price,
        boolean available,
        boolean availableForTestDrive) {

    public Car {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(vin, "vin is required");
        Objects.requireNonNull(carModelId, "carModelId is required");
        Objects.requireNonNull(color, "color is required");
        Objects.requireNonNull(price, "price is required");
    }

    public Car withAvailable(boolean available) {
        return new Car(id, vin, carModelId, color, price, available, availableForTestDrive);
    }

    public Car withAvailableForTestDrive(boolean availableForTestDrive) {
        return new Car(id, vin, carModelId, color, price, available, availableForTestDrive);
    }
}


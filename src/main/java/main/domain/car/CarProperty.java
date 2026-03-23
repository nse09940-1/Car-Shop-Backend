package main.domain.car;

import java.util.Objects;

public record CarProperty(
        CarType carType,
        FuelType fuelType,
        Engine engine,
        TransmissionType transmissionType,
        DriveType driveType) {

    public CarProperty {
        Objects.requireNonNull(carType, "bodyType is required");
        Objects.requireNonNull(fuelType, "fuelType is required");
        Objects.requireNonNull(engine, "engine is required");
        Objects.requireNonNull(transmissionType, "transmissionType is required");
        Objects.requireNonNull(driveType, "driveType is required");
    }
}


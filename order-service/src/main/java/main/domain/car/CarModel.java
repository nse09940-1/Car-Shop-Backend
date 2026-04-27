package main.domain.car;

import main.domain.Money;
import main.domain.configuration.ConfigType;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CarModel(
        UUID id,
        String brand,
        String modelCode,
        Money basePrice,
        CarProperty properties,
        Set<ConfigType> requiredTypes) {

    public CarModel {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(brand, "brand is required");
        Objects.requireNonNull(modelCode, "modelCode is required");
        Objects.requireNonNull(basePrice, "basePrice is required");
        Objects.requireNonNull(properties, "properties is required");
        requiredTypes = Set.copyOf(Objects.requireNonNull(requiredTypes, "requiredTypes is required"));
    }
}


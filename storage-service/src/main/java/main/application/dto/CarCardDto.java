package main.application.dto;

import main.domain.Money;

import java.util.UUID;

public record CarCardDto(
        UUID id,
        String vin,
        String brand,
        String model,
        String color,
        Money price,
        boolean available,
        boolean availableForTestDrive) {
}


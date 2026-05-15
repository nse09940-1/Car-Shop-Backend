package main.application.dto;

import main.domain.Money;

import java.util.UUID;

public record AvailableCarDto(
        UUID id,
        String vin,
        UUID carModelId,
        String color,
        Money price,
        boolean availableForTestDrive) {
}

package main.application.dto;

import main.domain.Money;
import main.domain.configuration.ConfigType;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PartDto(
        UUID id,
        String name,
        String partNumber,
        Money price,
        Set<String> compatibleModelCodes,
        ConfigType partType,
        int inStock,
        int reserved) {

    public PartDto {
        compatibleModelCodes = Set.copyOf(Objects.requireNonNull(compatibleModelCodes, "compatibleModelCodes is required"));
        Objects.requireNonNull(partType, "partType is required");
    }

    public PartDto(UUID id, String name, String partNumber, Money price, Set<String> compatibleModelCodes, ConfigType partType) {
        this(id, name, partNumber, price, compatibleModelCodes, partType, 0, 0);
    }
}


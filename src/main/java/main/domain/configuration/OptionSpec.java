package main.domain.configuration;

import main.domain.Money;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record OptionSpec(
        UUID id,
        String name,
        Money surcharge,
        Set<String> compatibleModelCodes,
        UUID carPartId,
        boolean baseOption) {

    public OptionSpec {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(surcharge, "surcharge is required");
        compatibleModelCodes = Set.copyOf(Objects.requireNonNull(compatibleModelCodes, "compatibleModelCodes is required"));
        Objects.requireNonNull(carPartId, "carPartId is required");
    }

    public boolean isCompatibleWith(String modelCode) {
        return compatibleModelCodes.contains(modelCode);
    }
}

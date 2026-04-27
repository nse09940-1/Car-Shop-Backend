package main.domain.car;

import main.domain.Money;
import main.domain.configuration.ConfigType;
import main.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CarPart(
        UUID id,
        String name,
        String partNumber,
        Money price,
        Set<String> compatibleModelCodes,
        ConfigType partType,
        int inStock,
        int reserved) {

    public CarPart {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(partNumber, "partNumber is required");
        Objects.requireNonNull(price, "price is required");
        compatibleModelCodes = Set.copyOf(Objects.requireNonNull(compatibleModelCodes, "compatibleModelCodes is required"));
        Objects.requireNonNull(partType, "partType is required");
        if (inStock < 0) throw new DomainValidationException("inStock must be non-negative");
        if (reserved < 0) throw new DomainValidationException("reserved must be non-negative");
        if (reserved > inStock) throw new DomainValidationException("reserved cannot exceed inStock");
    }

    public CarPart(UUID id, String name, String partNumber, Money price, Set<String> compatibleModelCodes, ConfigType partType) {
        this(id, name, partNumber, price, compatibleModelCodes, partType, 0, 0);
    }

    public int availableToReserve() {
        return inStock - reserved;
    }

    public CarPart reserve(int qty) {
        if (qty <= 0) {
            throw new DomainValidationException("Reservation quantity must be positive");
        }
        if (availableToReserve() < qty) {
            throw new DomainValidationException("Insufficient stock for part " + partNumber);
        }
        return new CarPart(id, name, partNumber, price, compatibleModelCodes, partType, inStock, reserved + qty);
    }

    public CarPart release(int qty) {
        if (qty <= 0) {
            throw new DomainValidationException("Release quantity must be positive");
        }
        if (reserved < qty) {
            throw new DomainValidationException("Cannot release more than reserved for part " + partNumber);
        }
        return new CarPart(id, name, partNumber, price, compatibleModelCodes, partType, inStock, reserved - qty);
    }

    public CarPart consume(int qty) {
        if (qty <= 0) {
            throw new DomainValidationException("Consume quantity must be positive");
        }
        if (reserved < qty) {
            throw new DomainValidationException("Cannot consume more than reserved for part " + partNumber);
        }
        return new CarPart(id, name, partNumber, price, compatibleModelCodes, partType, inStock - qty, reserved - qty);
    }
}

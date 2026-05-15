package main.application.mapper;

import main.application.dto.PartDto;
import main.domain.car.CarPart;

import java.util.UUID;

public final class AppMapper {

    public static PartDto toDto(CarPart carPart) {
        return new PartDto(
                carPart.id(),
                carPart.name(),
                carPart.partNumber(),
                carPart.price(),
                carPart.compatibleModelCodes(),
                carPart.partType(),
                carPart.inStock(),
                carPart.reserved());
    }

    public static CarPart toDomain(PartDto dto) {
        UUID id = dto.id() == null ? UUID.randomUUID() : dto.id();
        return new CarPart(id, dto.name(), dto.partNumber(), dto.price(), dto.compatibleModelCodes(), dto.partType(), dto.inStock(), dto.reserved());
    }

    public static CarPart toDomain(UUID id, PartDto dto) {
        return new CarPart(id, dto.name(), dto.partNumber(), dto.price(), dto.compatibleModelCodes(), dto.partType(), dto.inStock(), dto.reserved());
    }
}


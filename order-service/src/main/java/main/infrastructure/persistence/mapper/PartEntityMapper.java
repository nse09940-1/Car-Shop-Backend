package main.infrastructure.persistence.mapper;

import main.domain.Money;
import main.domain.car.CarPart;
import main.infrastructure.persistence.entity.PartJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartEntityMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    @Mapping(target = "price", expression = "java(part.price().rubles())")
    PartJpaEntity toEntity(CarPart part);

    default CarPart toDomain(PartJpaEntity entity) {
        return new CarPart(
                entity.getId(),
                entity.getName(),
                entity.getPartNumber(),
                new Money(entity.getPrice()),
                entity.getCompatibleModelCodes(),
                entity.getPartType(),
                entity.getInStock(),
                entity.getReserved());
    }
}

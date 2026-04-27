package main.infrastructure.persistence.mapper;

import main.domain.Money;
import main.domain.car.Car;
import main.infrastructure.persistence.entity.CarJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarEntityMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    @Mapping(target = "price", expression = "java(car.price().rubles())")
    CarJpaEntity toEntity(Car car);

    default Car toDomain(CarJpaEntity entity) {
        return new Car(
                entity.getId(),
                entity.getVin(),
                entity.getCarModelId(),
                entity.getColor(),
                new Money(entity.getPrice()),
                entity.isAvailable(),
                entity.isAvailableForTestDrive());
    }
}

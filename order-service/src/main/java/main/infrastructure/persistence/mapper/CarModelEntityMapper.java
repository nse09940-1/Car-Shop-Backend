package main.infrastructure.persistence.mapper;

import main.domain.Money;
import main.domain.car.CarModel;
import main.domain.car.CarProperty;
import main.domain.car.Engine;
import main.infrastructure.persistence.entity.CarModelJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarModelEntityMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    @Mapping(target = "basePrice", expression = "java(carModel.basePrice().rubles())")
    @Mapping(target = "carType", expression = "java(carModel.properties().carType())")
    @Mapping(target = "fuelType", expression = "java(carModel.properties().fuelType())")
    @Mapping(target = "horsepower", expression = "java(carModel.properties().engine().horsepower())")
    @Mapping(target = "engineVolume", expression = "java(carModel.properties().engine().engineVolume())")
    @Mapping(target = "transmissionType", expression = "java(carModel.properties().transmissionType())")
    @Mapping(target = "driveType", expression = "java(carModel.properties().driveType())")
    CarModelJpaEntity toEntity(CarModel carModel);

    default CarModel toDomain(CarModelJpaEntity entity) {
        return new CarModel(
                entity.getId(),
                entity.getBrand(),
                entity.getModelCode(),
                new Money(entity.getBasePrice()),
                new CarProperty(
                        entity.getCarType(),
                        entity.getFuelType(),
                        new Engine(entity.getHorsepower(), entity.getEngineVolume()),
                        entity.getTransmissionType(),
                        entity.getDriveType()),
                entity.getRequiredTypes());
    }
}

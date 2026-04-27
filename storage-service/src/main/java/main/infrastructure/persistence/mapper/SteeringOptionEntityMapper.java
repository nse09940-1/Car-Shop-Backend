package main.infrastructure.persistence.mapper;

import main.domain.Money;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.SteeringOption;
import main.infrastructure.persistence.entity.SteeringOptionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SteeringOptionEntityMapper {

    @Mapping(target = "id", expression = "java(option.spec().id())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    @Mapping(target = "name", expression = "java(option.spec().name())")
    @Mapping(target = "surcharge", expression = "java(option.spec().surcharge().rubles())")
    @Mapping(target = "baseOption", expression = "java(option.spec().baseOption())")
    @Mapping(target = "compatibleModelCodes", expression = "java(option.spec().compatibleModelCodes())")
    @Mapping(target = "carPartId", expression = "java(option.spec().carPartId())")
    SteeringOptionJpaEntity toEntity(SteeringOption option);

    default SteeringOption toDomain(SteeringOptionJpaEntity entity) {
        return new SteeringOption(
                new OptionSpec(
                        entity.getId(),
                        entity.getName(),
                        new Money(entity.getSurcharge()),
                        entity.getCompatibleModelCodes(),
                        entity.getCarPartId(),
                        entity.isBaseOption()),
                entity.getMaterial());
    }
}

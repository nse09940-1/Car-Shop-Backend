package main.infrastructure.persistence.mapper;

import main.domain.order.StockCarOrder;
import main.infrastructure.persistence.entity.StockOrderJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockOrderEntityMapper {
    @Mapping(target = "id", expression = "java(order.id())")
    @Mapping(target = "customerId", expression = "java(order.customerId())")
    @Mapping(target = "managerId", expression = "java(order.managerId())")
    @Mapping(target = "createdTime", expression = "java(order.createdAt())")
    @Mapping(target = "carId", expression = "java(order.carId())")
    @Mapping(target = "status", expression = "java(order.status())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    StockOrderJpaEntity toEntity(StockCarOrder order);

    default StockCarOrder toDomain(StockOrderJpaEntity entity) {
        return new StockCarOrder(
                entity.getId(),
                entity.getCustomerId(),
                entity.getManagerId(),
                entity.getCreatedTime(),
                entity.getCarId(),
                entity.getStatus());
    }
}

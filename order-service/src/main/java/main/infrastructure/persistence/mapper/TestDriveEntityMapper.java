package main.infrastructure.persistence.mapper;

import main.domain.TestDriveRequest;
import main.infrastructure.persistence.entity.TestDriveJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TestDriveEntityMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    TestDriveJpaEntity toEntity(TestDriveRequest request);

    default TestDriveRequest toDomain(TestDriveJpaEntity entity) {
        return new TestDriveRequest(
                entity.getId(),
                entity.getCustomerId(),
                entity.getCarId(),
                entity.getScheduledAt());
    }
}

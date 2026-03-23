package main.infrastructure.persistence.mapper;

import main.domain.user.User;
import main.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "removed", ignore = true)
    UserJpaEntity toEntity(User user);

    User toDomain(UserJpaEntity entity);
}

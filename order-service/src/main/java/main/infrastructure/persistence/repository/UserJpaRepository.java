package main.infrastructure.persistence.repository;

import main.domain.user.Role;
import main.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByIdAndRemovedFalse(UUID id);

    List<UserJpaEntity> findAllByRemovedFalse();

    List<UserJpaEntity> findAllByRoleAndRemovedFalse(Role role);
}


package main.infrastructure.persistence.adapter;

import main.application.port.repository.UserRepository;
import main.domain.user.Role;
import main.domain.user.User;
import main.infrastructure.persistence.entity.UserJpaEntity;
import main.infrastructure.persistence.mapper.UserEntityMapper;
import main.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository repository;
    private final UserEntityMapper mapper;

    public JpaUserRepositoryAdapter(UserJpaRepository repository, UserEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(User user) {
        UserJpaEntity existing = repository.findById(user.id()).orElse(null);
        UserJpaEntity entity = mapper.toEntity(user);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<User> findByRole(Role role) {
        return repository.findAllByRoleAndRemovedFalse(role).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.findByIdAndRemovedFalse(id).ifPresent(entity -> {
            entity.setRemoved(true);
            repository.save(entity);
        });
    }
}


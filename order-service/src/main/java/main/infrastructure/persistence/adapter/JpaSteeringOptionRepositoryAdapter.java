package main.infrastructure.persistence.adapter;

import main.application.port.repository.SteeringOptionRepository;
import main.domain.configuration.SteeringOption;
import main.infrastructure.persistence.entity.SteeringOptionJpaEntity;
import main.infrastructure.persistence.mapper.SteeringOptionEntityMapper;
import main.infrastructure.persistence.repository.SteeringOptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSteeringOptionRepositoryAdapter implements SteeringOptionRepository {
    private final SteeringOptionJpaRepository repository;
    private final SteeringOptionEntityMapper mapper;

    public JpaSteeringOptionRepositoryAdapter(SteeringOptionJpaRepository repository, SteeringOptionEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(SteeringOption option) {
        SteeringOptionJpaEntity existing = repository.findById(option.spec().id()).orElse(null);
        SteeringOptionJpaEntity entity = mapper.toEntity(option);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<SteeringOption> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<SteeringOption> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SteeringOption> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SteeringOption> findBaseByModelCode(String modelCode) {
        return repository.findBaseByModelCode(modelCode).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.findByIdAndRemovedFalse(id).ifPresent(entity -> {
            entity.setRemoved(true);
            repository.save(entity);
        });
    }
}


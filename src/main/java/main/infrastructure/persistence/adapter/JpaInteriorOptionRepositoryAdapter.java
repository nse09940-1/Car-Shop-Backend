package main.infrastructure.persistence.adapter;

import main.application.port.repository.InteriorOptionRepository;
import main.domain.configuration.InteriorOption;
import main.infrastructure.persistence.entity.InteriorOptionJpaEntity;
import main.infrastructure.persistence.mapper.InteriorOptionEntityMapper;
import main.infrastructure.persistence.repository.InteriorOptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaInteriorOptionRepositoryAdapter implements InteriorOptionRepository {
    private final InteriorOptionJpaRepository repository;
    private final InteriorOptionEntityMapper mapper;

    public JpaInteriorOptionRepositoryAdapter(InteriorOptionJpaRepository repository, InteriorOptionEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(InteriorOption option) {
        InteriorOptionJpaEntity existing = repository.findById(option.spec().id()).orElse(null);
        InteriorOptionJpaEntity entity = mapper.toEntity(option);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<InteriorOption> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<InteriorOption> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<InteriorOption> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<InteriorOption> findBaseByModelCode(String modelCode) {
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


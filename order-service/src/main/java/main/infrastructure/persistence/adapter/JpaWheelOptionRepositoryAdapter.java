package main.infrastructure.persistence.adapter;

import main.application.port.repository.WheelOptionRepository;
import main.domain.configuration.WheelOption;
import main.infrastructure.persistence.entity.WheelOptionJpaEntity;
import main.infrastructure.persistence.mapper.WheelOptionEntityMapper;
import main.infrastructure.persistence.repository.WheelOptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaWheelOptionRepositoryAdapter implements WheelOptionRepository {
    private final WheelOptionJpaRepository repository;
    private final WheelOptionEntityMapper mapper;

    public JpaWheelOptionRepositoryAdapter(WheelOptionJpaRepository repository, WheelOptionEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(WheelOption option) {
        WheelOptionJpaEntity existing = repository.findById(option.spec().id()).orElse(null);
        WheelOptionJpaEntity entity = mapper.toEntity(option);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<WheelOption> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<WheelOption> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<WheelOption> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WheelOption> findBaseByModelCode(String modelCode) {
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


package main.infrastructure.persistence.adapter;

import main.application.port.repository.TransmissionOptionRepository;
import main.domain.configuration.TransmissionOption;
import main.infrastructure.persistence.entity.TransmissionOptionJpaEntity;
import main.infrastructure.persistence.mapper.TransmissionOptionEntityMapper;
import main.infrastructure.persistence.repository.TransmissionOptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTransmissionOptionRepositoryAdapter implements TransmissionOptionRepository {
    private final TransmissionOptionJpaRepository repository;
    private final TransmissionOptionEntityMapper mapper;

    public JpaTransmissionOptionRepositoryAdapter(TransmissionOptionJpaRepository repository, TransmissionOptionEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(TransmissionOption option) {
        TransmissionOptionJpaEntity existing = repository.findById(option.spec().id()).orElse(null);
        TransmissionOptionJpaEntity entity = mapper.toEntity(option);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<TransmissionOption> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<TransmissionOption> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TransmissionOption> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TransmissionOption> findBaseByModelCode(String modelCode) {
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


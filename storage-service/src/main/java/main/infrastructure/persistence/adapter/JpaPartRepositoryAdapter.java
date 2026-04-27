package main.infrastructure.persistence.adapter;

import main.application.port.repository.PartRepository;
import main.domain.car.CarPart;
import main.infrastructure.persistence.entity.PartJpaEntity;
import main.infrastructure.persistence.mapper.PartEntityMapper;
import main.infrastructure.persistence.repository.PartJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPartRepositoryAdapter implements PartRepository {
    private final PartJpaRepository repository;
    private final PartEntityMapper mapper;

    public JpaPartRepositoryAdapter(PartJpaRepository repository, PartEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(CarPart carPart) {
        PartJpaEntity existing = repository.findById(carPart.id()).orElse(null);
        PartJpaEntity entity = mapper.toEntity(carPart);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<CarPart> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<CarPart> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.findByIdAndRemovedFalse(id).ifPresent(entity -> {
            entity.setRemoved(true);
            repository.save(entity);
        });
    }
}


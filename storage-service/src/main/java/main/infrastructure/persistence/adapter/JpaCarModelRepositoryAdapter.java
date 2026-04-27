package main.infrastructure.persistence.adapter;

import main.application.port.repository.CarModelRepository;
import main.domain.car.CarModel;
import main.infrastructure.persistence.entity.CarModelJpaEntity;
import main.infrastructure.persistence.mapper.CarModelEntityMapper;
import main.infrastructure.persistence.repository.CarModelJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCarModelRepositoryAdapter implements CarModelRepository {
    private final CarModelJpaRepository repository;
    private final CarModelEntityMapper mapper;

    public JpaCarModelRepositoryAdapter(CarModelJpaRepository repository, CarModelEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(CarModel carModel) {
        CarModelJpaEntity existing = repository.findById(carModel.id()).orElse(null);
        CarModelJpaEntity entity = mapper.toEntity(carModel);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<CarModel> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<CarModel> findAll() {
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


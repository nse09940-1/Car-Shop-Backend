package main.infrastructure.persistence.adapter;

import main.application.port.repository.CarRepository;
import main.domain.car.Car;
import main.infrastructure.persistence.entity.CarJpaEntity;
import main.infrastructure.persistence.mapper.CarEntityMapper;
import main.infrastructure.persistence.repository.CarJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCarRepositoryAdapter implements CarRepository {
    private final CarJpaRepository repository;
    private final CarEntityMapper mapper;

    public JpaCarRepositoryAdapter(CarJpaRepository repository, CarEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(Car car) {
        CarJpaEntity existing = repository.findById(car.id()).orElse(null);
        CarJpaEntity entity = mapper.toEntity(car);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<Car> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Car> findByVin(String vin) {
        return repository.findByVinAndRemovedFalse(vin).map(mapper::toDomain);
    }

    @Override
    public List<Car> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Car> findAllAvailable() {
        return repository.findAllByAvailableTrueAndRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Car> findAvailableById(UUID id) {
        return repository.findByIdAndAvailableTrueAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.findByIdAndRemovedFalse(id).ifPresent(entity -> {
            entity.setRemoved(true);
            repository.save(entity);
        });
    }
}


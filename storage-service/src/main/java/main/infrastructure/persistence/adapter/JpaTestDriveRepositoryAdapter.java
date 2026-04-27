package main.infrastructure.persistence.adapter;

import main.application.port.repository.TestDriveRepository;
import main.domain.TestDriveRequest;
import main.infrastructure.persistence.entity.TestDriveJpaEntity;
import main.infrastructure.persistence.mapper.TestDriveEntityMapper;
import main.infrastructure.persistence.repository.TestDriveJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTestDriveRepositoryAdapter implements TestDriveRepository {
    private final TestDriveJpaRepository repository;
    private final TestDriveEntityMapper mapper;

    public JpaTestDriveRepositoryAdapter(TestDriveJpaRepository repository, TestDriveEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(TestDriveRequest request) {
        TestDriveJpaEntity existing = repository.findById(request.id()).orElse(null);
        TestDriveJpaEntity entity = mapper.toEntity(request);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<TestDriveRequest> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<TestDriveRequest> findAll() {
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


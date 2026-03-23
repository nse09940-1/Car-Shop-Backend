package main.infrastructure.repository;

import main.application.port.repository.TestDriveRepository;
import main.domain.TestDriveRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryTestDriveRepository implements TestDriveRepository {
    private final Map<UUID, TestDriveRequest> storage = new HashMap<>();

    @Override
    public void save(TestDriveRequest request) {
        storage.put(request.id(), request);
    }

    @Override
    public Optional<TestDriveRequest> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<TestDriveRequest> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


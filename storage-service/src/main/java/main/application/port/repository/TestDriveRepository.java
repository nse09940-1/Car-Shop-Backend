package main.application.port.repository;

import main.domain.TestDriveRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestDriveRepository {
    void save(TestDriveRequest request);

    Optional<TestDriveRequest> findById(UUID id);

    List<TestDriveRequest> findAll();

    void deleteById(UUID id);
}


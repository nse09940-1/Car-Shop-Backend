package main.application.service;

import main.application.dto.AvailableCarDto;
import main.application.port.client.StorageCarCatalogClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CarAvailabilityService {
    private final StorageCarCatalogClient storageCarCatalogClient;

    public CarAvailabilityService(StorageCarCatalogClient storageCarCatalogClient) {
        this.storageCarCatalogClient = Objects.requireNonNull(storageCarCatalogClient, "storageCarCatalogClient is required");
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<AvailableCarDto> findAvailableCars() {
        return storageCarCatalogClient.findAvailableCars();
    }

    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public AvailableCarDto findAvailableCar(UUID id) {
        return storageCarCatalogClient.findAvailableCar(id);
    }
}

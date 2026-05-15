package main.application.port.client;

import main.application.dto.AvailableCarDto;

import java.util.List;
import java.util.UUID;

public interface StorageCarCatalogClient {
    List<AvailableCarDto> findAvailableCars();

    AvailableCarDto findAvailableCar(UUID id);
}

package main.infrastructure.client;

import main.application.port.client.StorageReadClient;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;
import java.util.UUID;

@Component
public class StorageReadHttpClient implements StorageReadClient {
    private final RestClient restClient;
    private final String baseUrl;

    public StorageReadHttpClient(RestClient.Builder restClientBuilder,
                                 @Value("${app.storage.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl is required");
    }

    @Override
    public StorageCarSnapshot getCar(UUID carId) {
        try {
            StorageCarResponse response = restClient.get()
                    .uri(baseUrl + "/internal/cars/{id}", carId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                        throw new EntityNotFoundException("Car not found");
                    })
                    .body(StorageCarResponse.class);
            if (response == null) {
                throw new EntityNotFoundException("Car not found");
            }
            return new StorageCarSnapshot(response.id(), response.available(), response.availableForTestDrive());
        } catch (RestClientResponseException ex) {
            throw new DomainValidationException("Storage service is unavailable");
        }
    }

    private record StorageCarResponse(
            UUID id,
            boolean available,
            boolean availableForTestDrive) {
    }
}

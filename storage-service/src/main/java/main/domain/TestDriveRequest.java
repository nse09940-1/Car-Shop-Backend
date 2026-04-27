package main.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record TestDriveRequest(
        UUID id,
        UUID customerId,
        UUID carId,
        LocalDateTime scheduledAt) {

    public TestDriveRequest {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(carId, "carId is required");
        Objects.requireNonNull(scheduledAt, "scheduledAt is required");
    }
}



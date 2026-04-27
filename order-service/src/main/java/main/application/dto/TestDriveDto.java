package main.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TestDriveDto(
        UUID id,
        UUID customerId,
        UUID carId,
        LocalDateTime scheduledAt) {
}


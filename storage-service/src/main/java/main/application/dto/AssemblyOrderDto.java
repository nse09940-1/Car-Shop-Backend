package main.application.dto;

import contracts.events.OrderType;
import main.domain.assembly.AssemblyOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssemblyOrderDto(
        UUID id,
        UUID sourceOrderId,
        OrderType sourceOrderType,
        UUID carId,
        UUID carModelId,
        UUID warehouseEmployeeId,
        AssemblyOrderStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        List<AssemblyRequiredPartDto> requiredParts) {
}

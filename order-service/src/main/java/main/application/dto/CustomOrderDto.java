package main.application.dto;

import main.domain.Money;
import main.domain.order.CustomOrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomOrderDto(
        UUID id,
        UUID customerId,
        UUID managerId,
        UUID carModelId,
        CustomOrderStatus status,
        Money totalPrice,
        LocalDateTime createdAt) {
}


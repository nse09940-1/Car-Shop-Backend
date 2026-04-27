package main.application.dto;

import main.domain.order.StockOrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockOrderDto(
        UUID id,
        UUID customerId,
        UUID managerId,
        UUID carId,
        StockOrderStatus status,
        LocalDateTime createdAt) {
}


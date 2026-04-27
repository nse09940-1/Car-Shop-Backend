package main.application.dto;

import java.util.UUID;

public record AssemblyRequiredPartDto(
        UUID partId,
        int quantity) {
}

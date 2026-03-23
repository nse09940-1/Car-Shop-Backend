package main.application.dto;

import main.domain.Money;

public record ConfigurationPriceDto(
        CarConfigurationDto configuration,
        Money totalPrice) {
}


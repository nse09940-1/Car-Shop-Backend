package main.application.dto;

import main.domain.configuration.ConfigType;

import java.util.Map;
import java.util.UUID;

public record BaseConfigurationDto(
        UUID modelId,
        String brand,
        String modelCode,
        Map<ConfigType, String> selectedOptions) {
}


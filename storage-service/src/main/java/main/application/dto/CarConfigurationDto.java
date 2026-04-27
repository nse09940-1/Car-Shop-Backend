package main.application.dto;

import main.domain.configuration.ConfigType;

import java.util.Map;
import java.util.UUID;

public record CarConfigurationDto(
        UUID modelId,
        Map<ConfigType, String> selectedOptions) {
}


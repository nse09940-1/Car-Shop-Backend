package main.domain.configuration;

import java.util.Objects;

public record SteeringOption(
        OptionSpec spec,
        String material) {

    public SteeringOption {
        Objects.requireNonNull(spec, "spec is required");
        Objects.requireNonNull(material, "material is required");
    }
}

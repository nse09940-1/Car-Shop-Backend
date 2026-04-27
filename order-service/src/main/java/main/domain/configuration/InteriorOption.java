package main.domain.configuration;

import java.util.Objects;

public record InteriorOption(
        OptionSpec spec,
        String color) {

    public InteriorOption {
        Objects.requireNonNull(spec, "spec is required");
        Objects.requireNonNull(color, "color is required");
    }
}

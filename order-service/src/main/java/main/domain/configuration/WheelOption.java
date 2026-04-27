package main.domain.configuration;

import java.util.Objects;

public record WheelOption(
        OptionSpec spec,
        int diameter
        ) {

    public WheelOption {
        Objects.requireNonNull(spec, "spec is required");
        if (diameter <= 0) throw new IllegalArgumentException("diameter must be positive");
    }
}

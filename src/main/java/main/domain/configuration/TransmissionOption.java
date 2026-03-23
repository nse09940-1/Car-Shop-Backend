package main.domain.configuration;

import main.domain.car.TransmissionType;

import java.util.Objects;

public record TransmissionOption(
        OptionSpec spec,
        TransmissionType transmissionType
        ) {

    public TransmissionOption {
        Objects.requireNonNull(spec, "spec is required");
        Objects.requireNonNull(transmissionType, "transmissionType is required");
    }
}

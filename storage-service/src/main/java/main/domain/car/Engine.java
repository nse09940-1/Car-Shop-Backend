package main.domain.car;

import main.domain.exception.DomainValidationException;

public record Engine(
        int horsepower,
        int engineVolume) {

    public Engine {
        if (horsepower <= 0) throw new DomainValidationException("Horsepower must be positive");
        if (engineVolume <= 0) throw new DomainValidationException("Engine volume must be positive");
    }
}

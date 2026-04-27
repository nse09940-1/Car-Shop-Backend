package main.domain.configuration;

import main.domain.exception.DomainValidationException;
import main.domain.Money;

import java.util.Set;

public class CarConfiguration {
    private final WheelOption wheelOption;
    private final TransmissionOption transmissionOption;
    private final SteeringOption steeringOption;
    private final InteriorOption interiorOption;

    public CarConfiguration(
            WheelOption wheelOption,
            TransmissionOption transmissionOption,
            SteeringOption steeringOption,
            InteriorOption interiorOption) {
        this.wheelOption = wheelOption;
        this.transmissionOption = transmissionOption;
        this.steeringOption = steeringOption;
        this.interiorOption = interiorOption;
    }

    public WheelOption wheelOption() {
        return wheelOption;
    }

    public TransmissionOption transmissionOption() {
        return transmissionOption;
    }

    public SteeringOption steeringOption() {
        return steeringOption;
    }

    public InteriorOption interiorOption() {
        return interiorOption;
    }

    public void validateRequiredNodes(Set<ConfigType> requiredNodes) {
        for (ConfigType node : requiredNodes) {
            boolean missing = switch (node) {
                case WHEELS -> wheelOption == null;
                case TRANSMISSION -> transmissionOption == null;
                case STEERING -> steeringOption == null;
                case INTERIOR -> interiorOption == null;
            };
            if (missing) {
                throw new DomainValidationException("Required option is missing: " + node);
            }
        }
    }

    public Money totalSurcharge() {
        Money total = new Money(0);
        if (wheelOption != null) total = total.add(wheelOption.spec().surcharge());
        if (transmissionOption != null) total = total.add(transmissionOption.spec().surcharge());
        if (steeringOption != null) total = total.add(steeringOption.spec().surcharge());
        if (interiorOption != null) total = total.add(interiorOption.spec().surcharge());
        return total;
    }
}


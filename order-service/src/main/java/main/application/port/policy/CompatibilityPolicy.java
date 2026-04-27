package main.application.port.policy;

import main.domain.configuration.OptionSpec;

public interface CompatibilityPolicy {
    boolean isCompatible(String modelCode, OptionSpec spec);
}


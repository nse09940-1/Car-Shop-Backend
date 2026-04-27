package main.infrastructure.policy;

import main.application.port.policy.CompatibilityPolicy;
import main.domain.configuration.OptionSpec;
import org.springframework.stereotype.Component;

@Component
public class DefaultCompatibilityPolicy implements CompatibilityPolicy {
    @Override
    public boolean isCompatible(String modelCode, OptionSpec spec) {
        return spec != null && spec.compatibleModelCodes().contains(modelCode);
    }
}


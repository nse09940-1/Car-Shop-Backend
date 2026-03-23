package main.application.service;

import main.application.dto.CarConfigurationDto;
import main.application.dto.ConfigurationPriceDto;
import main.application.mapper.AppMapper;
import main.application.port.policy.CompatibilityPolicy;
import main.application.port.repository.CarModelRepository;
import main.application.port.repository.InteriorOptionRepository;
import main.application.port.repository.SteeringOptionRepository;
import main.application.port.repository.TransmissionOptionRepository;
import main.application.port.repository.WheelOptionRepository;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.domain.exception.IncompatibleComponentException;
import main.domain.car.CarModel;
import main.domain.Money;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.configuration.InteriorOption;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.SteeringOption;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.WheelOption;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ConfiguratorService {
    private final CarModelRepository carModelRepository;
    private final WheelOptionRepository wheelOptionRepository;
    private final TransmissionOptionRepository transmissionOptionRepository;
    private final SteeringOptionRepository steeringOptionRepository;
    private final InteriorOptionRepository interiorOptionRepository;
    private final CompatibilityPolicy compatibilityPolicy;

    public ConfiguratorService(
            CarModelRepository carModelRepository,
            WheelOptionRepository wheelOptionRepository,
            TransmissionOptionRepository transmissionOptionRepository,
            SteeringOptionRepository steeringOptionRepository,
            InteriorOptionRepository interiorOptionRepository,
            CompatibilityPolicy compatibilityPolicy) {
        this.carModelRepository = Objects.requireNonNull(carModelRepository, "carModelRepository is required");
        this.wheelOptionRepository = Objects.requireNonNull(wheelOptionRepository, "wheelOptionRepository is required");
        this.transmissionOptionRepository = Objects.requireNonNull(transmissionOptionRepository, "transmissionOptionRepository is required");
        this.steeringOptionRepository = Objects.requireNonNull(steeringOptionRepository, "steeringOptionRepository is required");
        this.interiorOptionRepository = Objects.requireNonNull(interiorOptionRepository, "interiorOptionRepository is required");
        this.compatibilityPolicy = Objects.requireNonNull(compatibilityPolicy, "compatibilityPolicy is required");
    }

    public CarConfigurationDto getBaseConfiguration(UUID modelId) {
        CarModel carModel = getModelOrThrow(modelId);
        String modelCode = carModel.modelCode();
        Map<ConfigType, String> selected = new EnumMap<>(ConfigType.class);

        for (ConfigType node : carModel.requiredTypes()) {
            String baseName = switch (node) {
                case WHEELS -> wheelOptionRepository.findBaseByModelCode(modelCode)
                        .orElseThrow(() -> new DomainValidationException("Base option is missing: WHEELS"))
                        .spec().name();
                case TRANSMISSION -> transmissionOptionRepository.findBaseByModelCode(modelCode)
                        .orElseThrow(() -> new DomainValidationException("Base option is missing: TRANSMISSION"))
                        .spec().name();
                case STEERING -> steeringOptionRepository.findBaseByModelCode(modelCode)
                        .orElseThrow(() -> new DomainValidationException("Base option is missing: STEERING"))
                        .spec().name();
                case INTERIOR -> interiorOptionRepository.findBaseByModelCode(modelCode)
                        .orElseThrow(() -> new DomainValidationException("Base option is missing: INTERIOR"))
                        .spec().name();
            };
            selected.put(node, baseName);
        }
        return new CarConfigurationDto(carModel.id(), selected);
    }

    public AvailableOptions getAvailableOptions(UUID modelId) {
        CarModel carModel = getModelOrThrow(modelId);
        String modelCode = carModel.modelCode();
        return new AvailableOptions(
                wheelOptionRepository.findByModelCode(modelCode),
                transmissionOptionRepository.findByModelCode(modelCode),
                steeringOptionRepository.findByModelCode(modelCode),
                interiorOptionRepository.findByModelCode(modelCode));
    }

    public ConfigurationPriceDto buildConfiguration(UUID modelId, Map<ConfigType, UUID> selectedOptionIds) {
        BuiltConfiguration built = buildDomainConfiguration(modelId, selectedOptionIds);
        return AppMapper.toDto(built, modelId);
    }

    public BuiltConfiguration buildDomainConfiguration(UUID modelId, Map<ConfigType, UUID> selectedOptionIds) {
        CarModel carModel = getModelOrThrow(modelId);
        if (selectedOptionIds == null) {
            throw new DomainValidationException("Selected options are required");
        }
        String modelCode = carModel.modelCode();

        WheelOption wheel = null;
        TransmissionOption transmission = null;
        SteeringOption steering = null;
        InteriorOption interior = null;

        for (ConfigType node : carModel.requiredTypes()) {
            UUID optionId = selectedOptionIds.get(node);
            if (optionId == null) {
                throw new DomainValidationException("Required option is missing: " + node);
            }
            switch (node) {
                case WHEELS -> {
                    wheel = wheelOptionRepository.findById(optionId)
                            .orElseThrow(() -> new EntityNotFoundException("Wheel option not found"));
                    validateCompatibility(modelCode, wheel.spec());
                }
                case TRANSMISSION -> {
                    transmission = transmissionOptionRepository.findById(optionId)
                            .orElseThrow(() -> new EntityNotFoundException("Transmission option not found"));
                    validateCompatibility(modelCode, transmission.spec());
                }
                case STEERING -> {
                    steering = steeringOptionRepository.findById(optionId)
                            .orElseThrow(() -> new EntityNotFoundException("Steering option not found"));
                    validateCompatibility(modelCode, steering.spec());
                }
                case INTERIOR -> {
                    interior = interiorOptionRepository.findById(optionId)
                            .orElseThrow(() -> new EntityNotFoundException("Interior option not found"));
                    validateCompatibility(modelCode, interior.spec());
                }
            }
        }

        CarConfiguration configuration = new CarConfiguration(wheel, transmission, steering, interior);
        configuration.validateRequiredNodes(carModel.requiredTypes());
        Money totalPrice = carModel.basePrice().add(configuration.totalSurcharge());
        return new BuiltConfiguration(configuration, totalPrice);
    }

    private void validateCompatibility(String modelCode, OptionSpec spec) {
        if (!compatibilityPolicy.isCompatible(modelCode, spec)) {
            throw new IncompatibleComponentException("Option is not compatible with model " + modelCode);
        }
    }

    private CarModel getModelOrThrow(UUID modelId) {
        return carModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("Car model not found"));
    }

    public record BuiltConfiguration(
            CarConfiguration configuration,
            Money totalPrice) {
    }

    public record AvailableOptions(
            List<WheelOption> wheels,
            List<TransmissionOption> transmissions,
            List<SteeringOption> steerings,
            List<InteriorOption> interiors) {
    }
}


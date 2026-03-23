package main.presentation;

import main.application.dto.BaseConfigurationDto;
import main.application.dto.CarConfigurationDto;
import main.application.dto.ConfigurationPriceDto;
import main.application.service.BaseConfigurationQueryService;
import main.application.service.ConfiguratorService;
import main.domain.configuration.ConfigType;
import main.domain.configuration.InteriorOption;
import main.domain.configuration.SteeringOption;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.WheelOption;
import main.domain.exception.DomainValidationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/configurator")
public class ConfiguratorController {
    private final ConfiguratorService configuratorService;
    private final BaseConfigurationQueryService baseConfigurationQueryService;

    public ConfiguratorController(
            ConfiguratorService configuratorService,
            BaseConfigurationQueryService baseConfigurationQueryService) {
        this.configuratorService = configuratorService;
        this.baseConfigurationQueryService = baseConfigurationQueryService;
    }

    @GetMapping("/{modelId}/base")
    public CarConfigurationDto getBaseConfiguration(@PathVariable UUID modelId) {
        return configuratorService.getBaseConfiguration(modelId);
    }

    @GetMapping("/{modelId}/options")
    public AvailableOptionsResponse getAvailableOptions(@PathVariable UUID modelId) {
        ConfiguratorService.AvailableOptions options = configuratorService.getAvailableOptions(modelId);
        return AvailableOptionsResponse.from(options);
    }

    @PostMapping("/{modelId}/build")
    public ConfigurationPriceDto build(@PathVariable UUID modelId, @RequestBody BuildConfigurationRequest request) {
        return configuratorService.buildConfiguration(modelId, request.selectedOptionIds());
    }

    @GetMapping("/base-configurations")
    public List<BaseConfigurationDto> findBaseConfigurations(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false, name = "component") List<String> components) {
        List<BaseConfigurationQueryService.ComponentFilter> filters =
                components == null ? List.of() : components.stream().map(this::parseComponent).toList();
        return baseConfigurationQueryService.findBaseConfigurations(brand, filters);
    }

    private BaseConfigurationQueryService.ComponentFilter parseComponent(String raw) {
        String[] split = raw.split(":");
        if (split.length != 2) {
            throw new DomainValidationException("Component must be in format TYPE:UUID");
        }
        try {
            ConfigType type = ConfigType.valueOf(split[0].trim().toUpperCase());
            UUID optionId = UUID.fromString(split[1].trim());
            return new BaseConfigurationQueryService.ComponentFilter(type, optionId);
        } catch (RuntimeException ex) {
            throw new DomainValidationException("Invalid component filter: " + raw);
        }
    }

    public record BuildConfigurationRequest(Map<ConfigType, UUID> selectedOptionIds) {
    }

    public record AvailableOptionsResponse(
            List<WheelOptionResponse> wheels,
            List<TransmissionOptionResponse> transmissions,
            List<SteeringOptionResponse> steerings,
            List<InteriorOptionResponse> interiors) {
        static AvailableOptionsResponse from(ConfiguratorService.AvailableOptions source) {
            return new AvailableOptionsResponse(
                    source.wheels().stream().map(WheelOptionResponse::from).toList(),
                    source.transmissions().stream().map(TransmissionOptionResponse::from).toList(),
                    source.steerings().stream().map(SteeringOptionResponse::from).toList(),
                    source.interiors().stream().map(InteriorOptionResponse::from).toList());
        }
    }

    public record WheelOptionResponse(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            int diameter) {
        static WheelOptionResponse from(WheelOption option) {
            return new WheelOptionResponse(
                    option.spec().id(),
                    option.spec().name(),
                    option.spec().surcharge().rubles(),
                    option.spec().compatibleModelCodes(),
                    option.spec().carPartId(),
                    option.spec().baseOption(),
                    option.diameter());
        }
    }

    public record TransmissionOptionResponse(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            String transmissionType) {
        static TransmissionOptionResponse from(TransmissionOption option) {
            return new TransmissionOptionResponse(
                    option.spec().id(),
                    option.spec().name(),
                    option.spec().surcharge().rubles(),
                    option.spec().compatibleModelCodes(),
                    option.spec().carPartId(),
                    option.spec().baseOption(),
                    option.transmissionType().name());
        }
    }

    public record SteeringOptionResponse(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            String material) {
        static SteeringOptionResponse from(SteeringOption option) {
            return new SteeringOptionResponse(
                    option.spec().id(),
                    option.spec().name(),
                    option.spec().surcharge().rubles(),
                    option.spec().compatibleModelCodes(),
                    option.spec().carPartId(),
                    option.spec().baseOption(),
                    option.material());
        }
    }

    public record InteriorOptionResponse(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            String color) {
        static InteriorOptionResponse from(InteriorOption option) {
            return new InteriorOptionResponse(
                    option.spec().id(),
                    option.spec().name(),
                    option.spec().surcharge().rubles(),
                    option.spec().compatibleModelCodes(),
                    option.spec().carPartId(),
                    option.spec().baseOption(),
                    option.color());
        }
    }
}


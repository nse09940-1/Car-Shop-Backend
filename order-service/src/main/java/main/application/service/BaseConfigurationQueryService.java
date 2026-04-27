package main.application.service;

import main.application.dto.BaseConfigurationDto;
import main.domain.configuration.ConfigType;
import main.infrastructure.persistence.repository.CarModelJpaRepository;
import main.infrastructure.persistence.spec.CarModelSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BaseConfigurationQueryService {
    private final CarModelJpaRepository carModelJpaRepository;
    private final ConfiguratorService configuratorService;

    public BaseConfigurationQueryService(CarModelJpaRepository carModelJpaRepository, ConfiguratorService configuratorService) {
        this.carModelJpaRepository = carModelJpaRepository;
        this.configuratorService = configuratorService;
    }

    public List<BaseConfigurationDto> findBaseConfigurations(String brand, List<ComponentFilter> componentFilters) {
        Specification<main.infrastructure.persistence.entity.CarModelJpaEntity> specification =
                CarModelSpecifications.notRemoved().and(CarModelSpecifications.hasBrand(brand));

        if (componentFilters != null) {
            for (ComponentFilter componentFilter : componentFilters) {
                specification = specification.and(
                        CarModelSpecifications.hasBaseComponent(componentFilter.configType(), componentFilter.optionId()));
            }
        }

        return carModelJpaRepository.findAll(specification).stream()
                .map(entity -> {
                    Map<ConfigType, String> selected = configuratorService.getBaseConfiguration(entity.getId()).selectedOptions();
                    return new BaseConfigurationDto(entity.getId(), entity.getBrand(), entity.getModelCode(), selected);
                })
                .toList();
    }

    public record ComponentFilter(ConfigType configType, UUID optionId) {
    }
}


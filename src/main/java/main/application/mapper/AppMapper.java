package main.application.mapper;

import main.application.dto.CarCardDto;
import main.application.dto.CarConfigurationDto;
import main.application.dto.ConfigurationPriceDto;
import main.application.dto.CustomOrderDto;
import main.application.dto.PartDto;
import main.application.dto.StockOrderDto;
import main.application.dto.TestDriveDto;
import main.application.service.ConfiguratorService;
import main.domain.car.Car;
import main.domain.car.CarModel;
import main.domain.car.CarPart;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.ConfigType;
import main.domain.order.CustomCarOrder;
import main.domain.order.StockCarOrder;
import main.domain.TestDriveRequest;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class AppMapper {

    public static CarCardDto toDto(Car car, CarModel carModel) {
        return new CarCardDto(
                car.id(),
                car.vin(),
                carModel.brand(),
                carModel.modelCode(),
                car.color(),
                car.price(),
                car.available(),
                car.availableForTestDrive());
    }

    public static StockOrderDto toDto(StockCarOrder order) {
        return new StockOrderDto(
                order.id(),
                order.customerId(),
                order.managerId(),
                order.carId(),
                order.status(),
                order.createdAt());
    }

    public static CustomOrderDto toDto(CustomCarOrder order) {
        return new CustomOrderDto(
                order.id(),
                order.customerId(),
                order.managerId(),
                order.carModelId(),
                order.status(),
                order.totalPrice(),
                order.createdAt());
    }

    public static TestDriveDto toDto(TestDriveRequest request) {
        return new TestDriveDto(
                request.id(),
                request.customerId(),
                request.carId(),
                request.scheduledAt());
    }

    public static PartDto toDto(CarPart carPart) {
        return new PartDto(
                carPart.id(),
                carPart.name(),
                carPart.partNumber(),
                carPart.price(),
                carPart.compatibleModelCodes(),
                carPart.partType(),
                carPart.inStock(),
                carPart.reserved());
    }

    public static CarPart toDomain(PartDto dto) {
        UUID id = dto.id() == null ? UUID.randomUUID() : dto.id();
        return new CarPart(id, dto.name(), dto.partNumber(), dto.price(), dto.compatibleModelCodes(), dto.partType(), dto.inStock(), dto.reserved());
    }

    public static CarPart toDomain(UUID id, PartDto dto) {
        return new CarPart(id, dto.name(), dto.partNumber(), dto.price(), dto.compatibleModelCodes(), dto.partType(), dto.inStock(), dto.reserved());
    }

    public static ConfigurationPriceDto toDto(ConfiguratorService.BuiltConfiguration built, UUID modelId) {
        Map<ConfigType, String> selected = new EnumMap<>(ConfigType.class);
        CarConfiguration config = built.configuration();
        if (config.wheelOption() != null) {
            selected.put(ConfigType.WHEELS, config.wheelOption().spec().name());
        }
        if (config.transmissionOption() != null) {
            selected.put(ConfigType.TRANSMISSION, config.transmissionOption().spec().name());
        }
        if (config.steeringOption() != null) {
            selected.put(ConfigType.STEERING, config.steeringOption().spec().name());
        }
        if (config.interiorOption() != null) {
            selected.put(ConfigType.INTERIOR, config.interiorOption().spec().name());
        }
        CarConfigurationDto configDto = new CarConfigurationDto(modelId, selected);
        return new ConfigurationPriceDto(configDto, built.totalPrice());
    }
}


package main.presentation;

import main.application.dto.CustomOrderDto;
import main.application.dto.StockOrderDto;
import main.application.dto.TestDriveDto;
import main.application.service.SystemAdminService;
import main.domain.Money;
import main.domain.car.CarModel;
import main.domain.car.CarProperty;
import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.Engine;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import main.domain.configuration.ConfigType;
import main.domain.configuration.InteriorOption;
import main.domain.configuration.OptionSpec;
import main.domain.configuration.SteeringOption;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.WheelOption;
import main.domain.user.Role;
import main.domain.user.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final SystemAdminService systemAdminService;

    public AdminController(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
    }

    @PostMapping("/car-models")
    public void saveCarModel(@RequestBody CarModelSaveRequest request) {
        CarProperty property = new CarProperty(
                request.carType(),
                request.fuelType(),
                new Engine(request.horsepower(), request.engineVolume()),
                request.transmissionType(),
                request.driveType());
        CarModel carModel = new CarModel(
                request.id() == null ? UUID.randomUUID() : request.id(),
                request.brand(),
                request.modelCode(),
                new Money(request.basePrice()),
                property,
                request.requiredTypes());
        systemAdminService.saveCarModel(carModel);
    }

    @GetMapping("/car-models")
    public List<CarModelResponse> listCarModels() {
        return systemAdminService.listCarModels().stream().map(CarModelResponse::from).toList();
    }

    @DeleteMapping("/car-models/{id}")
    public void deleteCarModel(@PathVariable UUID id) {
        systemAdminService.deleteCarModel(id);
    }

    @PostMapping("/users")
    public void saveUser(@RequestBody UserSaveRequest request) {
        User user = new User(
                request.id() == null ? UUID.randomUUID() : request.id(),
                request.name(),
                request.email(),
                request.role());
        systemAdminService.saveUser(user);
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return systemAdminService.listUsers().stream().map(UserResponse::from).toList();
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable UUID id) {
        systemAdminService.deleteUser(id);
    }

    @PostMapping("/options/wheels")
    public void saveWheelOption(@RequestBody WheelOptionSaveRequest request) {
        WheelOption option = new WheelOption(toSpec(request), request.diameter());
        systemAdminService.saveWheelOption(option);
    }

    @GetMapping("/options/wheels")
    public List<WheelOptionResponse> listWheelOptions() {
        return systemAdminService.listWheelOptions().stream().map(WheelOptionResponse::from).toList();
    }

    @DeleteMapping("/options/wheels/{id}")
    public void deleteWheelOption(@PathVariable UUID id) {
        systemAdminService.deleteWheelOption(id);
    }

    @PostMapping("/options/transmissions")
    public void saveTransmissionOption(@RequestBody TransmissionOptionSaveRequest request) {
        TransmissionOption option = new TransmissionOption(toSpec(request), request.transmissionType());
        systemAdminService.saveTransmissionOption(option);
    }

    @GetMapping("/options/transmissions")
    public List<TransmissionOptionResponse> listTransmissionOptions() {
        return systemAdminService.listTransmissionOptions().stream().map(TransmissionOptionResponse::from).toList();
    }

    @DeleteMapping("/options/transmissions/{id}")
    public void deleteTransmissionOption(@PathVariable UUID id) {
        systemAdminService.deleteTransmissionOption(id);
    }

    @PostMapping("/options/steerings")
    public void saveSteeringOption(@RequestBody SteeringOptionSaveRequest request) {
        SteeringOption option = new SteeringOption(toSpec(request), request.material());
        systemAdminService.saveSteeringOption(option);
    }

    @GetMapping("/options/steerings")
    public List<SteeringOptionResponse> listSteeringOptions() {
        return systemAdminService.listSteeringOptions().stream().map(SteeringOptionResponse::from).toList();
    }

    @DeleteMapping("/options/steerings/{id}")
    public void deleteSteeringOption(@PathVariable UUID id) {
        systemAdminService.deleteSteeringOption(id);
    }

    @PostMapping("/options/interiors")
    public void saveInteriorOption(@RequestBody InteriorOptionSaveRequest request) {
        InteriorOption option = new InteriorOption(toSpec(request), request.color());
        systemAdminService.saveInteriorOption(option);
    }

    @GetMapping("/options/interiors")
    public List<InteriorOptionResponse> listInteriorOptions() {
        return systemAdminService.listInteriorOptions().stream().map(InteriorOptionResponse::from).toList();
    }

    @DeleteMapping("/options/interiors/{id}")
    public void deleteInteriorOption(@PathVariable UUID id) {
        systemAdminService.deleteInteriorOption(id);
    }

    @GetMapping("/orders/stock")
    public List<StockOrderDto> listStockOrders() {
        return systemAdminService.listStockOrders();
    }

    @DeleteMapping("/orders/stock/{id}")
    public void deleteStockOrder(@PathVariable UUID id) {
        systemAdminService.deleteStockOrder(id);
    }

    @GetMapping("/orders/custom")
    public List<CustomOrderDto> listCustomOrders() {
        return systemAdminService.listCustomOrders();
    }

    @DeleteMapping("/orders/custom/{id}")
    public void deleteCustomOrder(@PathVariable UUID id) {
        systemAdminService.deleteCustomOrder(id);
    }

    @GetMapping("/test-drives")
    public List<TestDriveDto> listTestDrives() {
        return systemAdminService.listTestDrives();
    }

    @DeleteMapping("/test-drives/{id}")
    public void deleteTestDrive(@PathVariable UUID id) {
        systemAdminService.deleteTestDrive(id);
    }

    private OptionSpec toSpec(OptionSaveRequest request) {
        return new OptionSpec(
                request.id() == null ? UUID.randomUUID() : request.id(),
                request.name(),
                new Money(request.surcharge()),
                request.compatibleModelCodes(),
                request.carPartId(),
                request.baseOption());
    }

    public record CarModelSaveRequest(
            UUID id,
            String brand,
            String modelCode,
            long basePrice,
            CarType carType,
            FuelType fuelType,
            int horsepower,
            int engineVolume,
            TransmissionType transmissionType,
            DriveType driveType,
            Set<ConfigType> requiredTypes) {
    }

    public record UserSaveRequest(UUID id, String name, String email, Role role) {
    }

    public interface OptionSaveRequest {
        UUID id();
        String name();
        long surcharge();
        Set<String> compatibleModelCodes();
        UUID carPartId();
        boolean baseOption();
    }

    public record WheelOptionSaveRequest(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            int diameter) implements OptionSaveRequest {
    }

    public record TransmissionOptionSaveRequest(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            TransmissionType transmissionType) implements OptionSaveRequest {
    }

    public record SteeringOptionSaveRequest(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            String material) implements OptionSaveRequest {
    }

    public record InteriorOptionSaveRequest(
            UUID id,
            String name,
            long surcharge,
            Set<String> compatibleModelCodes,
            UUID carPartId,
            boolean baseOption,
            String color) implements OptionSaveRequest {
    }

    public record CarModelResponse(
            UUID id,
            String brand,
            String modelCode,
            long basePrice,
            CarType carType,
            FuelType fuelType,
            int horsepower,
            int engineVolume,
            TransmissionType transmissionType,
            DriveType driveType,
            Set<ConfigType> requiredTypes) {
        static CarModelResponse from(CarModel model) {
            return new CarModelResponse(
                    model.id(),
                    model.brand(),
                    model.modelCode(),
                    model.basePrice().rubles(),
                    model.properties().carType(),
                    model.properties().fuelType(),
                    model.properties().engine().horsepower(),
                    model.properties().engine().engineVolume(),
                    model.properties().transmissionType(),
                    model.properties().driveType(),
                    model.requiredTypes());
        }
    }

    public record UserResponse(UUID id, String name, String email, Role role) {
        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.name(), user.email(), user.role());
        }
    }

    public record WheelOptionResponse(UUID id, String name, long surcharge, Set<String> compatibleModelCodes, UUID carPartId, boolean baseOption, int diameter) {
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

    public record TransmissionOptionResponse(UUID id, String name, long surcharge, Set<String> compatibleModelCodes, UUID carPartId, boolean baseOption, TransmissionType transmissionType) {
        static TransmissionOptionResponse from(TransmissionOption option) {
            return new TransmissionOptionResponse(
                    option.spec().id(),
                    option.spec().name(),
                    option.spec().surcharge().rubles(),
                    option.spec().compatibleModelCodes(),
                    option.spec().carPartId(),
                    option.spec().baseOption(),
                    option.transmissionType());
        }
    }

    public record SteeringOptionResponse(UUID id, String name, long surcharge, Set<String> compatibleModelCodes, UUID carPartId, boolean baseOption, String material) {
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

    public record InteriorOptionResponse(UUID id, String name, long surcharge, Set<String> compatibleModelCodes, UUID carPartId, boolean baseOption, String color) {
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

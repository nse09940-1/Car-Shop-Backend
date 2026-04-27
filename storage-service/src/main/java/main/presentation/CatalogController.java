package main.presentation;

import main.application.dto.CarCardDto;
import main.application.dto.CarFilterRequest;
import main.application.service.CarCatalogService;
import main.domain.Money;
import main.domain.car.CarType;
import main.domain.car.DriveType;
import main.domain.car.FuelType;
import main.domain.car.TransmissionType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cars")
public class CatalogController {
    private final CarCatalogService carCatalogService;

    public CatalogController(CarCatalogService carCatalogService) {
        this.carCatalogService = carCatalogService;
    }

    @GetMapping
    public List<CarCardDto> findAvailable(
            @RequestParam(required = false) Long priceMin,
            @RequestParam(required = false) Long priceMax,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) CarType carType,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Integer horsepowerMin,
            @RequestParam(required = false) Integer horsepowerMax,
            @RequestParam(required = false) Integer engineVolumeMin,
            @RequestParam(required = false) Integer engineVolumeMax,
            @RequestParam(required = false) TransmissionType transmissionType,
            @RequestParam(required = false) DriveType driveType,
            @RequestParam(required = false) String color) {

        CarFilterRequest filter = new CarFilterRequest(
                priceMin == null ? null : new Money(priceMin),
                priceMax == null ? null : new Money(priceMax),
                brand,
                model,
                carType,
                fuelType,
                horsepowerMin,
                horsepowerMax,
                engineVolumeMin,
                engineVolumeMax,
                transmissionType,
                driveType,
                color);

        return carCatalogService.findAvailable(filter);
    }

    @GetMapping("/{id}")
    public CarCardDto findById(@PathVariable UUID id) {
        return carCatalogService.findById(id);
    }
}


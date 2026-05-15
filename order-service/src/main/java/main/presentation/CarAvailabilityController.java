package main.presentation;

import main.application.dto.AvailableCarDto;
import main.application.service.CarAvailabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
public class CarAvailabilityController {
    private final CarAvailabilityService carAvailabilityService;

    public CarAvailabilityController(CarAvailabilityService carAvailabilityService) {
        this.carAvailabilityService = carAvailabilityService;
    }

    @GetMapping
    public List<AvailableCarDto> findAvailableCars() {
        return carAvailabilityService.findAvailableCars();
    }

    @GetMapping("/{id}")
    public AvailableCarDto findAvailableCar(@PathVariable UUID id) {
        return carAvailabilityService.findAvailableCar(id);
    }
}

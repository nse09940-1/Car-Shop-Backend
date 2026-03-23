package main.presentation;

import main.application.dto.CarCardDto;
import main.application.dto.TestDriveDto;
import main.application.service.TestDriveService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-drives")
public class TestDriveController {
    private final TestDriveService testDriveService;

    public TestDriveController(TestDriveService testDriveService) {
        this.testDriveService = testDriveService;
    }

    @PostMapping("/requests")
    public TestDriveDto createRequest(@RequestBody CreateTestDriveRequest request) {
        return testDriveService.createRequest(request.customerId(), request.carId(), request.scheduledAt());
    }

    @GetMapping("/requests")
    public List<TestDriveDto> findAllRequests() {
        return testDriveService.findAll();
    }

    @PostMapping("/cars/{carId}")
    public void addCarToTestDrive(@PathVariable UUID carId) {
        testDriveService.addCarToTestDriveList(carId);
    }

    @DeleteMapping("/cars/{carId}")
    public void removeCarFromTestDrive(@PathVariable UUID carId) {
        testDriveService.removeCarFromTestDriveList(carId);
    }

    @GetMapping("/cars")
    public List<CarCardDto> getTestDriveCars() {
        return testDriveService.getTestDriveCars();
    }

    public record CreateTestDriveRequest(UUID customerId, UUID carId, LocalDateTime scheduledAt) {
    }
}


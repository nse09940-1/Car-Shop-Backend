package main.presentation;

import main.application.dto.TestDriveDto;
import main.application.service.TestDriveService;
import org.springframework.web.bind.annotation.GetMapping;
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
        return testDriveService.createRequest(request.carId(), request.scheduledAt());
    }

    @GetMapping("/requests")
    public List<TestDriveDto> findAllRequests() {
        return testDriveService.findAll();
    }

    public record CreateTestDriveRequest(UUID carId, LocalDateTime scheduledAt) {
    }
}

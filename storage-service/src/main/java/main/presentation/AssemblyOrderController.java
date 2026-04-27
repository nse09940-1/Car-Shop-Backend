package main.presentation;

import contracts.events.OrderType;
import main.application.dto.AssemblyOrderDto;
import main.application.dto.AssemblyRequiredPartDto;
import main.application.service.AssemblyOrderService;
import main.domain.assembly.AssemblyOrderStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assembly-orders")
public class AssemblyOrderController {
    private final AssemblyOrderService assemblyOrderService;

    public AssemblyOrderController(AssemblyOrderService assemblyOrderService) {
        this.assemblyOrderService = assemblyOrderService;
    }

    @PostMapping
    public AssemblyOrderDto create(@RequestBody AssemblyOrderRequest request) {
        return assemblyOrderService.create(request.toDto());
    }

    @GetMapping
    public List<AssemblyOrderDto> findAll() {
        return assemblyOrderService.findAll();
    }

    @GetMapping("/{id}")
    public AssemblyOrderDto findById(@PathVariable UUID id) {
        return assemblyOrderService.findById(id);
    }

    @PutMapping("/{id}")
    public AssemblyOrderDto update(@PathVariable UUID id, @RequestBody AssemblyOrderRequest request) {
        return assemblyOrderService.update(id, request.toDto());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        assemblyOrderService.delete(id);
    }

    public record AssemblyOrderRequest(
            UUID sourceOrderId,
            OrderType sourceOrderType,
            UUID carId,
            UUID carModelId,
            UUID warehouseEmployeeId,
            AssemblyOrderStatus status,
            String failureReason,
            List<AssemblyRequiredPartDto> requiredParts) {
        AssemblyOrderDto toDto() {
            return new AssemblyOrderDto(
                    null,
                    sourceOrderId,
                    sourceOrderType,
                    carId,
                    carModelId,
                    warehouseEmployeeId,
                    status,
                    failureReason,
                    Instant.now(),
                    Instant.now(),
                    requiredParts == null ? List.of() : requiredParts);
        }
    }
}

package main.presentation;

import main.application.dto.CustomOrderDto;
import main.application.dto.StockOrderDto;
import main.application.service.OrderService;
import main.domain.configuration.ConfigType;
import main.domain.order.CustomOrderStatus;
import main.domain.order.StockOrderStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/stock")
    public StockOrderDto createStockOrder(@RequestBody CreateStockOrderRequest request) {
        return orderService.createStockOrder(request.carId());
    }

    @PatchMapping("/stock/{id}/status")
    public StockOrderDto changeStockStatus(@PathVariable UUID id, @RequestBody ChangeStockStatusRequest request) {
        return orderService.changeStockStatus(id, request.status());
    }

    @GetMapping("/stock/{id}")
    public StockOrderDto findStockById(@PathVariable UUID id) {
        return orderService.findStockById(id);
    }

    @GetMapping("/stock")
    public List<StockOrderDto> findAllStock() {
        return orderService.findAllStock();
    }

    @PostMapping("/custom")
    public CustomOrderDto createCustomOrder(@RequestBody CreateCustomOrderRequest request) {
        return orderService.createCustomOrder(request.carModelId(), request.selectedOptionIds());
    }

    @PatchMapping("/custom/{id}/status")
    public CustomOrderDto changeCustomStatus(@PathVariable UUID id, @RequestBody ChangeCustomStatusRequest request) {
        return orderService.changeCustomStatus(id, request.status());
    }

    @GetMapping("/custom/{id}")
    public CustomOrderDto findCustomById(@PathVariable UUID id) {
        return orderService.findCustomById(id);
    }

    @GetMapping("/custom")
    public List<CustomOrderDto> findAllCustom() {
        return orderService.findAllCustom();
    }

    public record CreateStockOrderRequest(UUID carId) {
    }

    public record ChangeStockStatusRequest(StockOrderStatus status) {
    }

    public record CreateCustomOrderRequest(UUID carModelId, Map<ConfigType, UUID> selectedOptionIds) {
    }

    public record ChangeCustomStatusRequest(CustomOrderStatus status) {
    }
}


package main.application.service;

import contracts.events.OrderSentForApprovalPayload;
import contracts.events.OrderType;
import contracts.events.RequiredPartItem;
import contracts.events.StockCarOperationPayload;
import main.application.dto.AssemblyOrderDto;
import main.application.dto.AssemblyRequiredPartDto;
import main.application.port.repository.AssemblyOrderRepository;
import main.application.port.repository.AssemblyOrderRequiredPartRepository;
import main.application.port.repository.CarRepository;
import main.application.port.repository.PartRepository;
import main.domain.assembly.AssemblyOrder;
import main.domain.assembly.AssemblyOrderStatus;
import main.domain.assembly.AssemblyRequiredPart;
import main.domain.car.Car;
import main.domain.car.CarPart;
import main.domain.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssemblyOrderService {
    private final AssemblyOrderRepository assemblyOrderRepository;
    private final AssemblyOrderRequiredPartRepository requiredPartRepository;
    private final CarRepository carRepository;
    private final PartRepository partRepository;

    public AssemblyOrderService(AssemblyOrderRepository assemblyOrderRepository,
                                AssemblyOrderRequiredPartRepository requiredPartRepository,
                                CarRepository carRepository,
                                PartRepository partRepository) {
        this.assemblyOrderRepository = Objects.requireNonNull(assemblyOrderRepository, "assemblyOrderRepository is required");
        this.requiredPartRepository = Objects.requireNonNull(requiredPartRepository, "requiredPartRepository is required");
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository is required");
    }

    @Transactional
    public AssemblyOrderDto create(AssemblyOrderDto request) {
        AssemblyOrder entity = new AssemblyOrder();
        apply(entity, request);
        AssemblyOrder saved = assemblyOrderRepository.save(entity);
        replaceRequiredParts(saved.getId(), request.requiredParts());
        return toDto(saved);
    }

    public List<AssemblyOrderDto> findAll() {
        return assemblyOrderRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public AssemblyOrderDto findById(UUID id) {
        return toDto(assemblyOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found")));
    }

    @Transactional
    public AssemblyOrderDto update(UUID id, AssemblyOrderDto request) {
        AssemblyOrder entity = assemblyOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found"));
        apply(entity, request);
        entity.setId(id);
        AssemblyOrder saved = assemblyOrderRepository.save(entity);
        replaceRequiredParts(id, request.requiredParts());
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        assemblyOrderRepository.deleteById(id);
    }

    @Transactional
    public ProcessingResult processApprovalRequest(UUID orderId, OrderType orderType, OrderSentForApprovalPayload payload) {
        AssemblyOrder entity = assemblyOrderRepository.findBySourceOrderId(orderId)
                .orElseGet(() -> createNew(orderId, orderType, payload));

        if (entity.getStatus() == AssemblyOrderStatus.ASSEMBLED
                || entity.getStatus() == AssemblyOrderStatus.RESERVED
                || entity.getStatus() == AssemblyOrderStatus.IN_PROGRESS) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() == AssemblyOrderStatus.FAIL) {
            return ProcessingResult.rejected(entity.getId(), entity.getFailureReason());
        }

        if (orderType == OrderType.STOCK) {
            return processStock(entity, payload);
        }
        return processCustom(entity, payload);
    }

    @Transactional
    public ProcessingResult processStockReservationRequest(UUID orderId, StockCarOperationPayload payload) {
        AssemblyOrder entity = assemblyOrderRepository.findBySourceOrderId(orderId)
                .orElseGet(() -> createStockNew(orderId, payload == null ? null : payload.carId()));

        if (entity.getStatus() == AssemblyOrderStatus.RESERVED || entity.getStatus() == AssemblyOrderStatus.ASSEMBLED) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() == AssemblyOrderStatus.FAIL) {
            return ProcessingResult.rejected(entity.getId(), entity.getFailureReason());
        }

        UUID carId = payload == null ? null : payload.carId();
        if (carId == null) {
            return fail(entity, "carId is required");
        }
        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) {
            return fail(entity, "Car not found");
        }
        if (!car.available()) {
            return fail(entity, "Car is not available");
        }

        carRepository.save(car.withAvailable(false));
        entity.setCarId(carId);
        entity.setStatus(AssemblyOrderStatus.RESERVED);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    @Transactional
    public ProcessingResult processStockWriteOffRequest(UUID orderId, StockCarOperationPayload payload) {
        AssemblyOrder entity = assemblyOrderRepository.findBySourceOrderId(orderId)
                .orElse(null);
        if (entity == null) {
            return ProcessingResult.rejected(null, "Assembly order not found");
        }
        if (entity.getStatus() == AssemblyOrderStatus.ASSEMBLED) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() == AssemblyOrderStatus.FAIL) {
            return ProcessingResult.rejected(entity.getId(), entity.getFailureReason());
        }
        if (entity.getStatus() != AssemblyOrderStatus.RESERVED) {
            return fail(entity, "Car is not reserved");
        }

        UUID carId = payload == null ? entity.getCarId() : payload.carId();
        if (carId == null) {
            return fail(entity, "carId is required");
        }
        if (carRepository.findById(carId).isEmpty()) {
            return fail(entity, "Car not found");
        }

        entity.setCarId(carId);
        entity.setStatus(AssemblyOrderStatus.ASSEMBLED);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    @Transactional
    public ProcessingResult processExecutionRequest(UUID orderId, OrderType orderType) {
        if (orderType != OrderType.CUSTOM) {
            return ProcessingResult.rejected(null, "Execution request is supported only for custom orders");
        }

        AssemblyOrder entity = assemblyOrderRepository.findBySourceOrderId(orderId)
                .orElse(null);
        if (entity == null) {
            return ProcessingResult.rejected(null, "Assembly order not found");
        }

        if (entity.getStatus() == AssemblyOrderStatus.IN_PROGRESS || entity.getStatus() == AssemblyOrderStatus.ASSEMBLED) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() != AssemblyOrderStatus.RESERVED) {
            return ProcessingResult.rejected(entity.getId(), "Assembly order is not reserved");
        }

        for (AssemblyRequiredPart item : requiredPartRepository.findAllByAssemblyOrderId(entity.getId())) {
            CarPart part = partRepository.findById(item.partId()).orElse(null);
            if (part == null) {
                return ProcessingResult.rejected(entity.getId(), "Part not found");
            }
            if (part.reserved() < item.quantity()) {
                return ProcessingResult.rejected(entity.getId(), "Cannot consume more than reserved for part " + part.partNumber());
            }
        }

        for (AssemblyRequiredPart item : requiredPartRepository.findAllByAssemblyOrderId(entity.getId())) {
            CarPart part = partRepository.findById(item.partId()).orElseThrow();
            partRepository.save(part.consume(item.quantity()));
        }

        entity.setStatus(AssemblyOrderStatus.IN_PROGRESS);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    @Transactional
    public ProcessingResult processReservationRelease(UUID orderId, OrderType orderType) {
        if (orderType == OrderType.CUSTOM) {
            return processCustomReservationRelease(orderId);
        }
        if (orderType == OrderType.STOCK) {
            return processStockReservationRelease(orderId);
        }
        return ProcessingResult.rejected(null, "Unsupported order type");
    }

    private ProcessingResult processCustomReservationRelease(UUID orderId) {
        AssemblyOrder entity = assemblyOrderRepository.findBySourceOrderId(orderId)
                .orElse(null);
        if (entity == null) {
            return ProcessingResult.rejected(null, "Assembly order not found");
        }
        if (entity.getStatus() == AssemblyOrderStatus.RELEASED) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() == AssemblyOrderStatus.IN_PROGRESS
                || entity.getStatus() == AssemblyOrderStatus.ASSEMBLED
                || entity.getStatus() == AssemblyOrderStatus.FAIL) {
            return ProcessingResult.rejected(entity.getId(), "Reservation cannot be released from status " + entity.getStatus());
        }
        if (entity.getStatus() != AssemblyOrderStatus.RESERVED) {
            return ProcessingResult.rejected(entity.getId(), "Assembly order is not reserved");
        }

        for (AssemblyRequiredPart item : requiredPartRepository.findAllByAssemblyOrderId(entity.getId())) {
            CarPart part = partRepository.findById(item.partId()).orElse(null);
            if (part == null) {
                return ProcessingResult.rejected(entity.getId(), "Part not found");
            }
            if (part.reserved() < item.quantity()) {
                return ProcessingResult.rejected(entity.getId(), "Cannot release more than reserved for part " + part.partNumber());
            }
        }

        for (AssemblyRequiredPart item : requiredPartRepository.findAllByAssemblyOrderId(entity.getId())) {
            CarPart part = partRepository.findById(item.partId()).orElseThrow();
            partRepository.save(part.release(item.quantity()));
        }

        entity.setStatus(AssemblyOrderStatus.RELEASED);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    private ProcessingResult processStockReservationRelease(UUID orderId) {
        AssemblyOrder entity = assemblyOrderRepository.findBySourceOrderId(orderId)
                .orElse(null);
        if (entity == null) {
            return ProcessingResult.rejected(null, "Assembly order not found");
        }
        if (entity.getStatus() == AssemblyOrderStatus.RELEASED) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() == AssemblyOrderStatus.ASSEMBLED
                || entity.getStatus() == AssemblyOrderStatus.IN_PROGRESS
                || entity.getStatus() == AssemblyOrderStatus.FAIL) {
            return ProcessingResult.rejected(entity.getId(), "Reservation cannot be released from status " + entity.getStatus());
        }
        if (entity.getStatus() != AssemblyOrderStatus.RESERVED) {
            return ProcessingResult.rejected(entity.getId(), "Car is not reserved");
        }
        if (entity.getCarId() == null) {
            return ProcessingResult.rejected(entity.getId(), "carId is required");
        }
        Car car = carRepository.findById(entity.getCarId()).orElse(null);
        if (car == null) {
            return ProcessingResult.rejected(entity.getId(), "Car not found");
        }

        carRepository.save(car.withAvailable(true));
        entity.setStatus(AssemblyOrderStatus.RELEASED);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    private AssemblyOrder createNew(UUID orderId, OrderType orderType, OrderSentForApprovalPayload payload) {
        AssemblyOrder entity = new AssemblyOrder();
        entity.setSourceOrderId(orderId);
        entity.setSourceOrderType(orderType);
        entity.setCarId(payload.carId());
        entity.setCarModelId(payload.carModelId());
        entity.setStatus(AssemblyOrderStatus.CREATED);
        AssemblyOrder saved = assemblyOrderRepository.save(entity);
        replaceRequiredParts(saved.getId(), toRequiredPartDtos(payload.requiredParts()));
        return saved;
    }

    private AssemblyOrder createStockNew(UUID orderId, UUID carId) {
        AssemblyOrder entity = new AssemblyOrder();
        entity.setSourceOrderId(orderId);
        entity.setSourceOrderType(OrderType.STOCK);
        entity.setCarId(carId);
        entity.setStatus(AssemblyOrderStatus.CREATED);
        return assemblyOrderRepository.save(entity);
    }

    private ProcessingResult processStock(AssemblyOrder entity, OrderSentForApprovalPayload payload) {
        if (payload.carId() == null) {
            return fail(entity, "carId is required");
        }
        Car car = carRepository.findById(payload.carId())
                .orElse(null);
        if (car == null) {
            return fail(entity, "Car not found");
        }
        if (!car.available()) {
            return fail(entity, "Car is not available");
        }
        carRepository.save(car.withAvailable(false));
        entity.setStatus(AssemblyOrderStatus.ASSEMBLED);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    private ProcessingResult processCustom(AssemblyOrder entity, OrderSentForApprovalPayload payload) {
        List<RequiredPartItem> requiredParts = payload.requiredParts() == null ? List.of() : payload.requiredParts();
        for (RequiredPartItem item : requiredParts) {
            CarPart part = partRepository.findById(item.partId()).orElse(null);
            if (part == null) {
                return fail(entity, "Part not found: " + item.partId());
            }
            if (part.availableToReserve() < item.quantity()) {
                return fail(entity, "Insufficient stock for part " + item.partId());
            }
        }

        for (RequiredPartItem item : requiredParts) {
            CarPart part = partRepository.findById(item.partId())
                    .orElseThrow(() -> new EntityNotFoundException("Part not found"));
            partRepository.save(part.reserve(item.quantity()));
        }

        entity.setStatus(AssemblyOrderStatus.RESERVED);
        entity.setFailureReason(null);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    private ProcessingResult fail(AssemblyOrder entity, String reason) {
        entity.setStatus(AssemblyOrderStatus.FAIL);
        entity.setFailureReason(reason);
        assemblyOrderRepository.save(entity);
        return ProcessingResult.rejected(entity.getId(), reason);
    }

    private void apply(AssemblyOrder entity, AssemblyOrderDto request) {
        entity.setSourceOrderId(request.sourceOrderId());
        entity.setSourceOrderType(request.sourceOrderType());
        entity.setCarId(request.carId());
        entity.setCarModelId(request.carModelId());
        entity.setWarehouseEmployeeId(request.warehouseEmployeeId());
        entity.setStatus(request.status() == null ? AssemblyOrderStatus.CREATED : request.status());
        entity.setFailureReason(request.failureReason());
    }

    private void replaceRequiredParts(UUID assemblyOrderId, List<AssemblyRequiredPartDto> requiredParts) {
        requiredPartRepository.replaceAll(assemblyOrderId, requiredParts == null ? List.of() : requiredParts.stream()
                .map(item -> new AssemblyRequiredPart(item.partId(), item.quantity()))
                .toList());
    }

    private List<AssemblyRequiredPartDto> toRequiredPartDtos(List<RequiredPartItem> requiredParts) {
        if (requiredParts == null) {
            return List.of();
        }
        return requiredParts.stream()
                .map(item -> new AssemblyRequiredPartDto(item.partId(), item.quantity()))
                .toList();
    }

    private AssemblyOrderDto toDto(AssemblyOrder entity) {
        return new AssemblyOrderDto(
                entity.getId(),
                entity.getSourceOrderId(),
                entity.getSourceOrderType(),
                entity.getCarId(),
                entity.getCarModelId(),
                entity.getWarehouseEmployeeId(),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                requiredPartRepository.findAllByAssemblyOrderId(entity.getId()).stream()
                        .map(item -> new AssemblyRequiredPartDto(item.partId(), item.quantity()))
                        .toList());
    }

    public record ProcessingResult(
            UUID assemblyOrderId,
            boolean approved,
            String reason) {
        public static ProcessingResult approved(UUID assemblyOrderId) {
            return new ProcessingResult(assemblyOrderId, true, null);
        }

        public static ProcessingResult rejected(UUID assemblyOrderId, String reason) {
            return new ProcessingResult(assemblyOrderId, false, reason);
        }
    }
}

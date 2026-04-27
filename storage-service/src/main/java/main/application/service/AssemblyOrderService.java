package main.application.service;

import contracts.events.OrderSentForApprovalPayload;
import contracts.events.OrderType;
import contracts.events.RequiredPartItem;
import contracts.events.StockCarOperationPayload;
import main.application.dto.AssemblyOrderDto;
import main.application.dto.AssemblyRequiredPartDto;
import main.application.port.repository.CarRepository;
import main.application.port.repository.PartRepository;
import main.domain.assembly.AssemblyOrderStatus;
import main.domain.car.Car;
import main.domain.car.CarPart;
import main.domain.exception.DomainValidationException;
import main.domain.exception.EntityNotFoundException;
import main.infrastructure.persistence.entity.AssemblyOrderJpaEntity;
import main.infrastructure.persistence.entity.AssemblyOrderRequiredPartJpaEntity;
import main.infrastructure.persistence.repository.AssemblyOrderJpaRepository;
import main.infrastructure.persistence.repository.AssemblyOrderRequiredPartJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssemblyOrderService {
    private final AssemblyOrderJpaRepository assemblyOrderJpaRepository;
    private final AssemblyOrderRequiredPartJpaRepository requiredPartJpaRepository;
    private final CarRepository carRepository;
    private final PartRepository partRepository;

    public AssemblyOrderService(AssemblyOrderJpaRepository assemblyOrderJpaRepository,
                                AssemblyOrderRequiredPartJpaRepository requiredPartJpaRepository,
                                CarRepository carRepository,
                                PartRepository partRepository) {
        this.assemblyOrderJpaRepository = Objects.requireNonNull(assemblyOrderJpaRepository, "assemblyOrderJpaRepository is required");
        this.requiredPartJpaRepository = Objects.requireNonNull(requiredPartJpaRepository, "requiredPartJpaRepository is required");
        this.carRepository = Objects.requireNonNull(carRepository, "carRepository is required");
        this.partRepository = Objects.requireNonNull(partRepository, "partRepository is required");
    }

    @Transactional
    public AssemblyOrderDto create(AssemblyOrderDto request) {
        AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
        apply(entity, request);
        entity.setRemoved(false);
        AssemblyOrderJpaEntity saved = assemblyOrderJpaRepository.save(entity);
        replaceRequiredParts(saved.getId(), request.requiredParts());
        return toDto(saved);
    }

    public List<AssemblyOrderDto> findAll() {
        return assemblyOrderJpaRepository.findAllByRemovedFalse().stream()
                .map(this::toDto)
                .toList();
    }

    public AssemblyOrderDto findById(UUID id) {
        return toDto(assemblyOrderJpaRepository.findByIdAndRemovedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found")));
    }

    @Transactional
    public AssemblyOrderDto update(UUID id, AssemblyOrderDto request) {
        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findByIdAndRemovedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found"));
        apply(entity, request);
        entity.setId(id);
        AssemblyOrderJpaEntity saved = assemblyOrderJpaRepository.save(entity);
        replaceRequiredParts(id, request.requiredParts());
        return toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findByIdAndRemovedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found"));
        entity.setRemoved(true);
        assemblyOrderJpaRepository.save(entity);
    }

    @Transactional
    public ProcessingResult processApprovalRequest(UUID orderId, OrderType orderType, OrderSentForApprovalPayload payload) {
        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findBySourceOrderIdAndRemovedFalse(orderId)
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
        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findBySourceOrderIdAndRemovedFalse(orderId)
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
        assemblyOrderJpaRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    @Transactional
    public ProcessingResult processStockWriteOffRequest(UUID orderId, StockCarOperationPayload payload) {
        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findBySourceOrderIdAndRemovedFalse(orderId)
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
        assemblyOrderJpaRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    @Transactional
    public ProcessingResult processExecutionRequest(UUID orderId, OrderType orderType) {
        if (orderType != OrderType.CUSTOM) {
            throw new DomainValidationException("Execution request is supported only for custom orders");
        }

        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findBySourceOrderIdAndRemovedFalse(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Assembly order not found"));

        if (entity.getStatus() == AssemblyOrderStatus.IN_PROGRESS || entity.getStatus() == AssemblyOrderStatus.ASSEMBLED) {
            return ProcessingResult.approved(entity.getId());
        }
        if (entity.getStatus() != AssemblyOrderStatus.RESERVED) {
            throw new DomainValidationException("Assembly order is not reserved");
        }

        for (AssemblyOrderRequiredPartJpaEntity item : requiredPartJpaRepository.findAllByAssemblyOrderId(entity.getId())) {
            CarPart part = partRepository.findById(item.getPartId())
                    .orElseThrow(() -> new EntityNotFoundException("Part not found"));
            partRepository.save(part.consume(item.getQuantity()));
        }

        entity.setStatus(AssemblyOrderStatus.IN_PROGRESS);
        entity.setFailureReason(null);
        assemblyOrderJpaRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    @Transactional
    public void processReservationRelease(UUID orderId, OrderType orderType) {
        if (orderType != OrderType.CUSTOM) {
            return;
        }

        AssemblyOrderJpaEntity entity = assemblyOrderJpaRepository.findBySourceOrderIdAndRemovedFalse(orderId)
                .orElse(null);
        if (entity == null
                || entity.getStatus() == AssemblyOrderStatus.RELEASED
                || entity.getStatus() == AssemblyOrderStatus.IN_PROGRESS
                || entity.getStatus() == AssemblyOrderStatus.ASSEMBLED
                || entity.getStatus() == AssemblyOrderStatus.FAIL) {
            return;
        }
        if (entity.getStatus() != AssemblyOrderStatus.RESERVED) {
            return;
        }

        for (AssemblyOrderRequiredPartJpaEntity item : requiredPartJpaRepository.findAllByAssemblyOrderId(entity.getId())) {
            CarPart part = partRepository.findById(item.getPartId())
                    .orElseThrow(() -> new EntityNotFoundException("Part not found"));
            partRepository.save(part.release(item.getQuantity()));
        }

        entity.setStatus(AssemblyOrderStatus.RELEASED);
        entity.setFailureReason(null);
        assemblyOrderJpaRepository.save(entity);
    }

    private AssemblyOrderJpaEntity createNew(UUID orderId, OrderType orderType, OrderSentForApprovalPayload payload) {
        AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
        entity.setSourceOrderId(orderId);
        entity.setSourceOrderType(orderType);
        entity.setCarId(payload.carId());
        entity.setCarModelId(payload.carModelId());
        entity.setStatus(AssemblyOrderStatus.CREATED);
        entity.setRemoved(false);
        AssemblyOrderJpaEntity saved = assemblyOrderJpaRepository.save(entity);
        replaceRequiredParts(saved.getId(), toRequiredPartDtos(payload.requiredParts()));
        return saved;
    }

    private AssemblyOrderJpaEntity createStockNew(UUID orderId, UUID carId) {
        AssemblyOrderJpaEntity entity = new AssemblyOrderJpaEntity();
        entity.setSourceOrderId(orderId);
        entity.setSourceOrderType(OrderType.STOCK);
        entity.setCarId(carId);
        entity.setStatus(AssemblyOrderStatus.CREATED);
        entity.setRemoved(false);
        return assemblyOrderJpaRepository.save(entity);
    }

    private ProcessingResult processStock(AssemblyOrderJpaEntity entity, OrderSentForApprovalPayload payload) {
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
        assemblyOrderJpaRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    private ProcessingResult processCustom(AssemblyOrderJpaEntity entity, OrderSentForApprovalPayload payload) {
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
        assemblyOrderJpaRepository.save(entity);
        return ProcessingResult.approved(entity.getId());
    }

    private ProcessingResult fail(AssemblyOrderJpaEntity entity, String reason) {
        entity.setStatus(AssemblyOrderStatus.FAIL);
        entity.setFailureReason(reason);
        assemblyOrderJpaRepository.save(entity);
        return ProcessingResult.rejected(entity.getId(), reason);
    }

    private void apply(AssemblyOrderJpaEntity entity, AssemblyOrderDto request) {
        entity.setSourceOrderId(request.sourceOrderId());
        entity.setSourceOrderType(request.sourceOrderType());
        entity.setCarId(request.carId());
        entity.setCarModelId(request.carModelId());
        entity.setWarehouseEmployeeId(request.warehouseEmployeeId());
        entity.setStatus(request.status() == null ? AssemblyOrderStatus.CREATED : request.status());
        entity.setFailureReason(request.failureReason());
    }

    private void replaceRequiredParts(UUID assemblyOrderId, List<AssemblyRequiredPartDto> requiredParts) {
        requiredPartJpaRepository.deleteAllByAssemblyOrderId(assemblyOrderId);
        if (requiredParts == null) {
            return;
        }
        for (AssemblyRequiredPartDto item : requiredParts) {
            AssemblyOrderRequiredPartJpaEntity entity = new AssemblyOrderRequiredPartJpaEntity();
            entity.setAssemblyOrderId(assemblyOrderId);
            entity.setPartId(item.partId());
            entity.setQuantity(item.quantity());
            requiredPartJpaRepository.save(entity);
        }
    }

    private List<AssemblyRequiredPartDto> toRequiredPartDtos(List<RequiredPartItem> requiredParts) {
        if (requiredParts == null) {
            return List.of();
        }
        return requiredParts.stream()
                .map(item -> new AssemblyRequiredPartDto(item.partId(), item.quantity()))
                .toList();
    }

    private AssemblyOrderDto toDto(AssemblyOrderJpaEntity entity) {
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
                requiredPartJpaRepository.findAllByAssemblyOrderId(entity.getId()).stream()
                        .map(item -> new AssemblyRequiredPartDto(item.getPartId(), item.getQuantity()))
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

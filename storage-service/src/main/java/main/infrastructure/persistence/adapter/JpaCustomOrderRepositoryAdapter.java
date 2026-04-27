package main.infrastructure.persistence.adapter;

import main.application.port.repository.CustomOrderRepository;
import main.domain.Money;
import main.domain.configuration.CarConfiguration;
import main.domain.configuration.InteriorOption;
import main.domain.configuration.SteeringOption;
import main.domain.configuration.TransmissionOption;
import main.domain.configuration.WheelOption;
import main.domain.order.CustomCarOrder;
import main.infrastructure.persistence.entity.CustomOrderJpaEntity;
import main.infrastructure.persistence.mapper.InteriorOptionEntityMapper;
import main.infrastructure.persistence.mapper.SteeringOptionEntityMapper;
import main.infrastructure.persistence.mapper.TransmissionOptionEntityMapper;
import main.infrastructure.persistence.mapper.WheelOptionEntityMapper;
import main.infrastructure.persistence.repository.CustomOrderJpaRepository;
import main.infrastructure.persistence.repository.InteriorOptionJpaRepository;
import main.infrastructure.persistence.repository.SteeringOptionJpaRepository;
import main.infrastructure.persistence.repository.TransmissionOptionJpaRepository;
import main.infrastructure.persistence.repository.WheelOptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCustomOrderRepositoryAdapter implements CustomOrderRepository {
    private final CustomOrderJpaRepository repository;
    private final WheelOptionJpaRepository wheelOptionJpaRepository;
    private final TransmissionOptionJpaRepository transmissionOptionJpaRepository;
    private final SteeringOptionJpaRepository steeringOptionJpaRepository;
    private final InteriorOptionJpaRepository interiorOptionJpaRepository;
    private final WheelOptionEntityMapper wheelMapper;
    private final TransmissionOptionEntityMapper transmissionMapper;
    private final SteeringOptionEntityMapper steeringMapper;
    private final InteriorOptionEntityMapper interiorMapper;

    public JpaCustomOrderRepositoryAdapter(
            CustomOrderJpaRepository repository,
            WheelOptionJpaRepository wheelOptionJpaRepository,
            TransmissionOptionJpaRepository transmissionOptionJpaRepository,
            SteeringOptionJpaRepository steeringOptionJpaRepository,
            InteriorOptionJpaRepository interiorOptionJpaRepository,
            WheelOptionEntityMapper wheelMapper,
            TransmissionOptionEntityMapper transmissionMapper,
            SteeringOptionEntityMapper steeringMapper,
            InteriorOptionEntityMapper interiorMapper) {
        this.repository = repository;
        this.wheelOptionJpaRepository = wheelOptionJpaRepository;
        this.transmissionOptionJpaRepository = transmissionOptionJpaRepository;
        this.steeringOptionJpaRepository = steeringOptionJpaRepository;
        this.interiorOptionJpaRepository = interiorOptionJpaRepository;
        this.wheelMapper = wheelMapper;
        this.transmissionMapper = transmissionMapper;
        this.steeringMapper = steeringMapper;
        this.interiorMapper = interiorMapper;
    }

    @Override
    public void save(CustomCarOrder order) {
        CustomOrderJpaEntity existing = repository.findById(order.id()).orElse(null);
        CustomOrderJpaEntity entity = toEntity(order);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<CustomCarOrder> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(this::toDomain);
    }

    @Override
    public List<CustomCarOrder> findAll() {
        return repository.findAllByRemovedFalse().stream().map(this::toDomain).toList();
    }

    @Override
    public List<CustomCarOrder> findAllByCustomerId(UUID customerId) {
        return repository.findAllByCustomerIdAndRemovedFalse(customerId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean isOwner(UUID orderId, UUID customerId) {
        return repository.existsByIdAndCustomerIdAndRemovedFalse(orderId, customerId);
    }

    @Override
    public void deleteById(UUID id) {
        repository.findByIdAndRemovedFalse(id).ifPresent(entity -> {
            entity.setRemoved(true);
            repository.save(entity);
        });
    }

    private CustomOrderJpaEntity toEntity(CustomCarOrder order) {
        CarConfiguration configuration = order.configuration();
        CustomOrderJpaEntity entity = new CustomOrderJpaEntity();
        entity.setId(order.id());
        entity.setCustomerId(order.customerId());
        entity.setManagerId(order.managerId());
        entity.setCarModelId(order.carModelId());
        entity.setStatus(order.status());
        entity.setTotalPrice(order.totalPrice().rubles());
        entity.setCreatedTime(order.createdAt());
        entity.setWheelOptionId(configuration.wheelOption() == null ? null : configuration.wheelOption().spec().id());
        entity.setTransmissionOptionId(configuration.transmissionOption() == null ? null : configuration.transmissionOption().spec().id());
        entity.setSteeringOptionId(configuration.steeringOption() == null ? null : configuration.steeringOption().spec().id());
        entity.setInteriorOptionId(configuration.interiorOption() == null ? null : configuration.interiorOption().spec().id());
        return entity;
    }

    private CustomCarOrder toDomain(CustomOrderJpaEntity entity) {
        WheelOption wheelOption = entity.getWheelOptionId() == null ? null : wheelOptionJpaRepository.findByIdAndRemovedFalse(entity.getWheelOptionId())
                .map(wheelMapper::toDomain)
                .orElse(null);
        TransmissionOption transmissionOption = entity.getTransmissionOptionId() == null ? null : transmissionOptionJpaRepository.findByIdAndRemovedFalse(entity.getTransmissionOptionId())
                .map(transmissionMapper::toDomain)
                .orElse(null);
        SteeringOption steeringOption = entity.getSteeringOptionId() == null ? null : steeringOptionJpaRepository.findByIdAndRemovedFalse(entity.getSteeringOptionId())
                .map(steeringMapper::toDomain)
                .orElse(null);
        InteriorOption interiorOption = entity.getInteriorOptionId() == null ? null : interiorOptionJpaRepository.findByIdAndRemovedFalse(entity.getInteriorOptionId())
                .map(interiorMapper::toDomain)
                .orElse(null);

        CarConfiguration configuration = new CarConfiguration(wheelOption, transmissionOption, steeringOption, interiorOption);
        return new CustomCarOrder(
                entity.getId(),
                entity.getCustomerId(),
                entity.getManagerId(),
                entity.getCreatedTime(),
                entity.getCarModelId(),
                configuration,
                new Money(entity.getTotalPrice()),
                entity.getStatus());
    }
}


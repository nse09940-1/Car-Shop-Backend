package main.infrastructure.persistence.adapter;

import main.application.port.repository.StockOrderRepository;
import main.domain.order.StockCarOrder;
import main.infrastructure.persistence.entity.StockOrderJpaEntity;
import main.infrastructure.persistence.mapper.StockOrderEntityMapper;
import main.infrastructure.persistence.repository.StockOrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStockOrderRepositoryAdapter implements StockOrderRepository {
    private final StockOrderJpaRepository repository;
    private final StockOrderEntityMapper mapper;

    public JpaStockOrderRepositoryAdapter(StockOrderJpaRepository repository, StockOrderEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void save(StockCarOrder order) {
        StockOrderJpaEntity existing = repository.findById(order.id()).orElse(null);
        StockOrderJpaEntity entity = mapper.toEntity(order);
        PersistenceService.preserve(existing, entity);
        repository.save(entity);
    }

    @Override
    public Optional<StockCarOrder> findById(UUID id) {
        return repository.findByIdAndRemovedFalse(id).map(mapper::toDomain);
    }

    @Override
    public List<StockCarOrder> findAll() {
        return repository.findAllByRemovedFalse().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<StockCarOrder> findAllByCustomerId(UUID customerId) {
        return repository.findAllByCustomerIdAndRemovedFalse(customerId).stream().map(mapper::toDomain).toList();
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
}


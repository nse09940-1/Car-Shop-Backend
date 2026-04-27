package main.application.port.repository;

import main.domain.order.StockCarOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockOrderRepository {
    void save(StockCarOrder order);

    Optional<StockCarOrder> findById(UUID id);

    List<StockCarOrder> findAll();

    List<StockCarOrder> findAllByCustomerId(UUID customerId);

    boolean isOwner(UUID orderId, UUID customerId);

    void deleteById(UUID id);
}


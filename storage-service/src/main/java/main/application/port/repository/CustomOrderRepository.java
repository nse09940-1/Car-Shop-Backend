package main.application.port.repository;

import main.domain.order.CustomCarOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomOrderRepository {
    void save(CustomCarOrder order);

    Optional<CustomCarOrder> findById(UUID id);

    List<CustomCarOrder> findAll();

    List<CustomCarOrder> findAllByCustomerId(UUID customerId);

    boolean isOwner(UUID orderId, UUID customerId);

    void deleteById(UUID id);
}


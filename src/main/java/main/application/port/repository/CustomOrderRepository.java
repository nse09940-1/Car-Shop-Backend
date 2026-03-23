package main.application.port.repository;

import main.domain.order.CustomCarOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomOrderRepository {
    void save(CustomCarOrder order);

    Optional<CustomCarOrder> findById(UUID id);

    List<CustomCarOrder> findAll();

    void deleteById(UUID id);
}


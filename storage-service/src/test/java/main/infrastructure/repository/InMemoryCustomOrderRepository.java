package main.infrastructure.repository;

import main.application.port.repository.CustomOrderRepository;
import main.domain.order.CustomCarOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCustomOrderRepository implements CustomOrderRepository {
    private final Map<UUID, CustomCarOrder> storage = new HashMap<>();

    @Override
    public void save(CustomCarOrder order) {
        storage.put(order.id(), order);
    }

    @Override
    public Optional<CustomCarOrder> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<CustomCarOrder> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<CustomCarOrder> findAllByCustomerId(UUID customerId) {
        return storage.values().stream()
                .filter(order -> order.customerId().equals(customerId))
                .toList();
    }

    @Override
    public boolean isOwner(UUID orderId, UUID customerId) {
        CustomCarOrder order = storage.get(orderId);
        return order != null && order.customerId().equals(customerId);
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


package main.infrastructure.repository;

import main.application.port.repository.StockOrderRepository;
import main.domain.order.StockCarOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryStockOrderRepository implements StockOrderRepository {
    private final Map<UUID, StockCarOrder> storage = new HashMap<>();

    @Override
    public void save(StockCarOrder order) {
        storage.put(order.id(), order);
    }

    @Override
    public Optional<StockCarOrder> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<StockCarOrder> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


package main.infrastructure.repository;

import main.application.port.repository.UserRepository;
import main.domain.user.Role;
import main.domain.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryUserRepository implements UserRepository {
    private final Map<UUID, User> storage = new HashMap<>();

    @Override
    public void save(User user) {
        storage.put(user.id(), user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<User> findByRole(Role role) {
        return storage.values()
                .stream()
                .filter(user -> user.role() == role)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}


package main.application.port.repository;

import main.domain.user.Role;
import main.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    void save(User user);

    Optional<User> findById(UUID id);

    List<User> findAll();

    List<User> findByRole(Role role);

    void deleteById(UUID id);
}


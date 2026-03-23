package main.infrastructure.policy;

import main.application.port.policy.ManagerAssignmentPolicy;
import main.domain.exception.DomainValidationException;
import main.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@Component
public class RandomManagerAssignmentPolicy implements ManagerAssignmentPolicy {
    private final Random random;

    public RandomManagerAssignmentPolicy() {
        this(new Random());
    }

    public RandomManagerAssignmentPolicy(Random random) {
        this.random = Objects.requireNonNull(random, "random is required");
    }

    @Override
    public User assignManager(List<User> managers) {
        if (managers == null || managers.isEmpty()) {
            throw new DomainValidationException("No manager available");
        }
        return managers.get(random.nextInt(managers.size()));
    }
}


package main.application.port.policy;

import main.domain.user.User;

import java.util.List;

public interface ManagerAssignmentPolicy {
    User assignManager(List<User> managers);
}


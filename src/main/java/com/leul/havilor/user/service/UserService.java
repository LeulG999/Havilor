package com.leul.havilor.user.service;

import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.user.api.response.UserResponse;
import com.leul.havilor.user.domain.enums.Role;
import com.leul.havilor.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// What other modules may ask about users (the border rule: they never touch UserRepository).
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id).map(UserResponse::from)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    // Names for a list of user ids (e.g. the assignees on one page of alerts), in ONE query.
    // Nulls are ignored; an unknown id is simply missing from the map.
    @Transactional(readOnly = true)
    public Map<Long, String> namesByIds(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        List<Long> known = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (!known.isEmpty()) {
            userRepository.findAllById(known).forEach(user -> names.put(user.getId(), user.getName()));
        }
        return names;
    }

    // Active users with this role, by name (e.g. the analysts a manager can assign an alert to).
    @Transactional(readOnly = true)
    public List<UserResponse> activeByRole(Role role) {
        return userRepository.findByRoleAndActiveTrueOrderByName(role).stream().map(UserResponse::from).toList();
    }

    // Can alerts be assigned to this user? Only an active ANALYST (Flow F25).
    @Transactional(readOnly = true)
    public boolean isActiveAnalyst(Long id) {
        return userRepository.findById(id).map(user -> user.isActive() && user.getRole() == Role.ANALYST).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }
}

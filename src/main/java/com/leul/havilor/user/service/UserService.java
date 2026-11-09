package com.leul.havilor.user.service;

import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.user.api.response.UserResponse;
import com.leul.havilor.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }
}

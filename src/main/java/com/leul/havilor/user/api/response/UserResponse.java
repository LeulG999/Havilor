package com.leul.havilor.user.api.response;

import com.leul.havilor.user.domain.User;

// A user as the API shows it. Never the password hash.
public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isActive());
    }
}

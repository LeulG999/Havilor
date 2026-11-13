package com.leul.havilor.user.api.request;

import com.leul.havilor.user.domain.enums.Role;

// PATCH /api/v1/users/{id} (MANAGER only). Both optional: null means "don't change it".
public record UpdateUserRequest(
        Boolean active,
        Role role
) {
}

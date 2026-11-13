package com.leul.havilor.user.api.response;

// The answer to POST /api/v1/users: the new person and their temporary password. This is the ONLY time the
// password is in plain text anywhere: only its BCrypt hash is stored, and no endpoint returns it again.
public record CreatedUserResponse(
        UserResponse user,
        String temporaryPassword
) {
}

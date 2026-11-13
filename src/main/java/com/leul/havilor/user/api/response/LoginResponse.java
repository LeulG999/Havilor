package com.leul.havilor.user.api.response;

// The access token to send as "Authorization: Bearer <accessToken>", and how many seconds it stays valid.
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword   // true: the only thing to do is set a new password (Flow F30)
) {
}

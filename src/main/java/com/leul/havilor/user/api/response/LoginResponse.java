package com.leul.havilor.user.api.response;

// The access token to send as "Authorization: Bearer <accessToken>", and how many seconds it stays valid.
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}

package com.leul.havilor.alert.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

// A click in POST /api/v1/alerts. The recipient and URL don't have ids yet, so they are named by address
// and must match an entry in the same request's recipients and urls lists.
public record ClickRequest(
        @NotBlank @Size(max = 320) String recipientAddress,
        @NotBlank @Size(max = 2048) String urlAddress,
        @NotNull Instant clickedAt,
        @NotNull Boolean permitted
) {
}

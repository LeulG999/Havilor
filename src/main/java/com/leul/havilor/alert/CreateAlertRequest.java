package com.leul.havilor.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

// What a caller sends to POST /api/v1/alerts. Status is not here: the server decides it.
// The limits match the database columns, so anything that passes validation also fits the table.
public record CreateAlertRequest(
        @NotBlank @Size(max = 50) String ticketNumber,
        @NotBlank @Size(max = 500) String subject,
        @NotNull Instant receivedAt
) {
}

package com.leul.havilor.alert;

import java.time.Instant;

// What a caller sends to POST /api/v1/alerts. Status is not here: the server decides it.
// Validation annotations (@NotBlank, ...) come in Flow 3.
public record CreateAlertRequest(
        String ticketNumber,
        String subject,
        Instant receivedAt
) {
}

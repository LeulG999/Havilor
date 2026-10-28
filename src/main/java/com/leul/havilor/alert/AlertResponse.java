package com.leul.havilor.alert;

import java.time.Instant;

// What the API returns for an alert. The entity never leaves the service.
public record AlertResponse(
        Long id,
        String ticketNumber,
        String subject,
        String status,
        Instant receivedAt
) {

    static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTicketNumber(),
                alert.getSubject(),
                alert.getStatus(),
                alert.getReceivedAt());
    }
}

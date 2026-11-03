package com.leul.havilor.alert;

import com.leul.havilor.common.CaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

// What a caller sends to POST /api/v1/alerts. Status, verdict and suggestedVerdict are not here:
// the server decides them. Assignment and "waiting on" come later through PATCH.
// The limits match the database columns, so anything that passes validation also fits the table.
public record CreateAlertRequest(
        @NotBlank @Size(max = 50) String ticketNumber,
        @NotNull Source source,
        @NotNull CaseType caseType,
        @NotNull Priority priority,
        @NotNull Instant receivedAt,
        @NotNull @Valid EmailDetailsRequest email
) {
}

package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.Priority;
import com.leul.havilor.alert.domain.Source;
import com.leul.havilor.common.CaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

// What a caller sends to POST /api/v1/alerts. Status, verdict and suggestedVerdict are not here:
// the server decides them. Assignment and "waiting on" come later through PATCH.
// The limits match the database columns, so anything that passes validation also fits the table.
// The list caps stop one request from inserting an unbounded number of rows.
// Data only: AlertFactory (service/) turns it into an Alert.
public record CreateAlertRequest(
        @NotBlank @Size(max = 50) String ticketNumber,
        @NotNull Source source,
        @NotNull CaseType caseType,
        @NotNull Priority priority,
        @NotNull Instant receivedAt,
        @NotNull @Valid EmailDetailsRequest email,
        @NotEmpty @Size(max = 500) List<@NotNull @Valid RecipientRequest> recipients,
        @Size(max = 500) List<@NotNull @Valid UrlRequest> urls,
        @Size(max = 100) List<@NotNull @Valid AttachmentRequest> attachments,
        @Size(max = 1000) List<@NotNull @Valid ClickRequest> clicks
) {

    // Lists that may be left out arrive as null; treat that as "none".
    public CreateAlertRequest {
        urls = urls == null ? List.of() : urls;
        attachments = attachments == null ? List.of() : attachments;
        clicks = clicks == null ? List.of() : clicks;
    }
}

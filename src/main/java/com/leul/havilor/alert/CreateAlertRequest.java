package com.leul.havilor.alert;

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
public record CreateAlertRequest(
        @NotBlank @Size(max = 50) String ticketNumber,
        @NotNull Source source,
        @NotNull CaseType caseType,
        @NotNull Priority priority,
        @NotNull Instant receivedAt,
        @NotNull @Valid EmailDetailsRequest email,
        @NotEmpty @Size(max = 500) List<@NotNull @Valid RecipientRequest> recipients,
        @Size(max = 500) List<@NotNull @Valid UrlRequest> urls,
        @Size(max = 100) List<@NotNull @Valid AttachmentRequest> attachments
) {

    // Lists that may be left out arrive as null; treat that as "none".
    public CreateAlertRequest {
        urls = urls == null ? List.of() : urls;
        attachments = attachments == null ? List.of() : attachments;
    }

    Alert toAlert() {
        Alert alert = new Alert(ticketNumber, source, caseType, priority, receivedAt, email.toEmailDetails());
        recipients.forEach(r -> alert.addRecipient(r.toRecipient()));
        urls.forEach(u -> alert.addUrl(u.toUrl()));
        attachments.forEach(a -> alert.addAttachment(a.toAttachment()));
        return alert;
    }
}

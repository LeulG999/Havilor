package com.leul.havilor.alert;

import com.leul.havilor.common.BadRequestException;
import com.leul.havilor.common.CaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        @Size(max = 100) List<@NotNull @Valid AttachmentRequest> attachments,
        @Size(max = 1000) List<@NotNull @Valid ClickRequest> clicks
) {

    // Lists that may be left out arrive as null; treat that as "none".
    public CreateAlertRequest {
        urls = urls == null ? List.of() : urls;
        attachments = attachments == null ? List.of() : attachments;
        clicks = clicks == null ? List.of() : clicks;
    }

    // Builds the whole graph. A click must name a recipient and a URL from this same request;
    // anything else is a 400 (BadRequestException), because the fields are valid one by one but don't fit.
    Alert toAlert() {
        Alert alert = new Alert(ticketNumber, source, caseType, priority, receivedAt, email.toEmailDetails());
        recipients.forEach(r -> alert.addRecipient(r.toRecipient()));
        urls.forEach(u -> alert.addUrl(u.toUrl()));
        attachments.forEach(a -> alert.addAttachment(a.toAttachment()));

        // Email addresses are case-insensitive; URLs are compared exactly.
        Map<String, Recipient> recipientsByAddress = alert.getRecipients().stream()
                .collect(Collectors.toMap(r -> r.getAddress().toLowerCase(Locale.ROOT), Function.identity(),
                        (first, duplicate) -> first));
        Map<String, Url> urlsByAddress = alert.getUrls().stream()
                .collect(Collectors.toMap(Url::getAddress, Function.identity(), (first, duplicate) -> first));
        for (int i = 0; i < clicks.size(); i++) {
            ClickRequest click = clicks.get(i);
            Recipient recipient = recipientsByAddress.get(click.recipientAddress().toLowerCase(Locale.ROOT));
            if (recipient == null) {
                throw new BadRequestException("clicks[" + i + "].recipientAddress", "is not one of the recipients");
            }
            Url url = urlsByAddress.get(click.urlAddress());
            if (url == null) {
                throw new BadRequestException("clicks[" + i + "].urlAddress", "is not one of the urls");
            }
            url.addClick(recipient, click.clickedAt(), click.permitted());
        }
        return alert;
    }
}

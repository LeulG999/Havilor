package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.enums.AuthResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The email part of POST /api/v1/alerts. Only subject and fromAddress are required: the other fields depend on
// what the source system (ServiceNow, Proofpoint TAP) knows. Addresses are NOT checked with @Email on purpose:
// a malformed sender is evidence, not bad input.
public record EmailDetailsRequest(
        @NotBlank @Size(max = 500) String subject,
        @NotBlank @Size(max = 320) String fromAddress,
        @Size(max = 320) String envelopeSender,
        @Size(max = 500) String senderHeader,
        @Size(max = 320) String replyTo,
        @Size(max = 200) String xMailer,
        AuthResult spfResult,
        AuthResult dkimResult,
        AuthResult dmarcResult,
        @Min(0) @Max(100) Integer phishScore,
        @Min(0) @Max(100) Integer spamScore,
        @Size(max = 50) String filterAction,
        @Size(max = 100) String quarantineFolder,
        @Size(max = 100_000) String rawHeaders
) {
}

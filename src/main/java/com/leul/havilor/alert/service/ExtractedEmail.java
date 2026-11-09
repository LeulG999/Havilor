package com.leul.havilor.alert.service;

import com.leul.havilor.alert.domain.enums.AuthResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// What Claude extracts from a reported email (Flow 19): the structured-output type of the manual intake.
// Optional fields become "string or null" in the JSON schema, so Claude can say "not shown" instead of inventing
// a value. AlertIntakeService turns it into a draft CreateAlertRequest.
public record ExtractedEmail(
        String subject,
        String fromAddress,
        Optional<String> replyTo,
        Optional<String> envelopeSender,
        Optional<AuthResult> spfResult,
        Optional<AuthResult> dkimResult,
        Optional<AuthResult> dmarcResult,
        Optional<Instant> receivedAt,
        List<String> recipients,
        List<String> urls,
        List<String> attachmentFileNames
) {
}

package com.leul.havilor.alert;

import com.leul.havilor.common.CaseType;

import java.time.Instant;
import java.time.LocalDate;

// What the API returns for an alert. The entity never leaves the service.
public record AlertResponse(
        Long id,
        String ticketNumber,
        Source source,
        CaseType caseType,
        Priority priority,
        AlertStatus status,
        Verdict verdict,
        Verdict suggestedVerdict,
        String suggestionReason,
        Long assigneeId,
        String waitingOn,
        String externalCaseNumber,
        LocalDate followUpOn,
        Instant receivedAt,
        Instant closedAt,
        String summary,
        EmailDetailsResponse email,
        Instant createdAt,
        Instant updatedAt
) {

    static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTicketNumber(),
                alert.getSource(),
                alert.getCaseType(),
                alert.getPriority(),
                alert.getStatus(),
                alert.getVerdict(),
                alert.getSuggestedVerdict(),
                alert.getSuggestionReason(),
                alert.getAssigneeId(),
                alert.getWaitingOn(),
                alert.getExternalCaseNumber(),
                alert.getFollowUpOn(),
                alert.getReceivedAt(),
                alert.getClosedAt(),
                alert.getSummary(),
                EmailDetailsResponse.from(alert.getEmail()),
                alert.getCreatedAt(),
                alert.getUpdatedAt());
    }
}

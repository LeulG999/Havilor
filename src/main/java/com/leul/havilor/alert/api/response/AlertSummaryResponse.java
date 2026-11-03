package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.domain.enums.Source;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.common.CaseType;
import java.time.Instant;

// One row in the alert queue (GET /api/v1/alerts). Only columns of the alert table itself:
// no children, so a page of 20 alerts is one query (plus the count), never 1 + 20 × 3.
public record AlertSummaryResponse(
        Long id,
        String ticketNumber,
        Source source,
        CaseType caseType,
        Priority priority,
        AlertStatus status,
        Verdict verdict,
        Long assigneeId,
        String subject,
        String fromAddress,
        Instant receivedAt
) {

    public static AlertSummaryResponse from(Alert alert) {
        return new AlertSummaryResponse(
                alert.getId(),
                alert.getTicketNumber(),
                alert.getSource(),
                alert.getCaseType(),
                alert.getPriority(),
                alert.getStatus(),
                alert.getVerdict(),
                alert.getAssigneeId(),
                alert.getEmail().getSubject(),
                alert.getEmail().getFromAddress(),
                alert.getReceivedAt());
    }
}

package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.ClickEvent;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.domain.enums.Source;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.common.CaseType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// GET /api/v1/alerts/{id} and POST: the whole alert with its children. The entity never leaves the service.
public record AlertDetailResponse(
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
        Long playbookId,
        Integer playbookVersion,
        EmailDetailsResponse email,
        List<RecipientResponse> recipients,
        List<UrlResponse> urls,
        List<AttachmentResponse> attachments,
        List<ClickResponse> clicks,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public static AlertDetailResponse from(Alert alert, List<ClickEvent> clicks) {
        return new AlertDetailResponse(
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
                alert.getPlaybookId(),
                alert.getPlaybookVersion(),
                EmailDetailsResponse.from(alert.getEmail()),
                alert.getRecipients().stream().map(RecipientResponse::from).toList(),
                alert.getUrls().stream().map(UrlResponse::from).toList(),
                alert.getAttachments().stream().map(AttachmentResponse::from).toList(),
                clicks.stream().map(ClickResponse::from).toList(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getVersion());
    }
}

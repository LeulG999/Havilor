package com.leul.havilor.finding.api.response;

import com.leul.havilor.finding.domain.Finding;

import java.time.Instant;

// type, severity and source are Strings, so other modules (e.g. the AI assessment in alert) can read a finding
// without depending on finding.domain (the border rule).
public record FindingResponse(
        Long id,
        Long alertId,
        String type,
        String severity,
        String title,
        String teachingText,
        String source,
        Instant createdAt
) {

    public static FindingResponse from(Finding finding) {
        return new FindingResponse(finding.getId(), finding.getAlertId(), finding.getType().name(),
                finding.getSeverity().name(), finding.getTitle(), finding.getTeachingText(),
                finding.getSource().name(), finding.getCreatedAt());
    }
}

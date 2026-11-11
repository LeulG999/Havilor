package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.enums.Verdict;

import java.util.List;

// Claude's assessment of an alert, plus the reference knowledge it was given (similar closed alerts, playbook steps).
public record AiAssessmentResponse(
        String summary,
        Verdict suggestedVerdict,
        String suggestionReason,
        List<Reference> references
) {

    // sourceType: CLOSED_ALERT (sourceId = an alert id) or PLAYBOOK_STEP (sourceId = a playbook step id).
    public record Reference(String sourceType, Long sourceId) {
    }
}

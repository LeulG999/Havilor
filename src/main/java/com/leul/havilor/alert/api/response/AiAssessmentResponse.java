package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.alert.service.AiAssessment;

import java.util.List;

// Claude's assessment of an alert (Flow F27), plus the reference knowledge it was given (similar closed alerts,
// playbook steps). The same JSON is saved on the alert as lastAssessment.
public record AiAssessmentResponse(
        Verdict suggestedVerdict,
        AiAssessment.Level confidence,
        String oneLineSummary,
        List<AiAssessment.Evidence> keyEvidence,
        List<String> nextSteps,
        String wouldChangeMyMindIf,
        List<Reference> references
) {

    // sourceType: CLOSED_ALERT (sourceId = an alert id) or PLAYBOOK_STEP (sourceId = a playbook step id).
    public record Reference(String sourceType, Long sourceId) {
    }
}

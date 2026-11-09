package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.enums.Verdict;

// Claude's assessment of an alert. Also the structured-output type: the SDK derives the JSON schema Claude must
// answer in from this record, so the answer always has these three fields and a valid Verdict.
public record AiAssessmentResponse(
        String summary,
        Verdict suggestedVerdict,
        String suggestionReason
) {
}

package com.leul.havilor.alert.service;

import com.leul.havilor.alert.domain.enums.Verdict;

// Claude's answer to an assessment: the structured-output type. The SDK derives the JSON schema from this record,
// so the answer always has these three fields and a valid Verdict. AlertAiService adds the references.
public record AiAssessment(
        String summary,
        Verdict suggestedVerdict,
        String suggestionReason
) {
}

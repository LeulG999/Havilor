package com.leul.havilor.alert.service;

import com.leul.havilor.alert.domain.enums.Verdict;

import java.util.List;

// Claude's answer to an assessment: the structured-output type. The SDK derives the JSON schema from this record, so
// the answer always has these fields, a valid Verdict and valid levels. AlertAiService caps the list lengths and adds
// the references (Flow F27: short, readable parts instead of paragraphs).
public record AiAssessment(
        Verdict suggestedVerdict,
        Level confidence,             // how sure Claude is of that verdict
        String oneLineSummary,        // one sentence, at most ~25 words
        List<Evidence> keyEvidence,   // at most 6, most important first
        List<String> nextSteps,       // at most 5 short imperative lines
        String wouldChangeMyMindIf    // one line
) {

    public enum Level {
        LOW,
        MEDIUM,
        HIGH
    }

    // One piece of evidence in plain words, and how strongly it points to a threat.
    public record Evidence(String text, Level severity) {
    }
}

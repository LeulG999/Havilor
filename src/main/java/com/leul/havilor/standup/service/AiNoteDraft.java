package com.leul.havilor.standup.service;

import java.util.List;

// Claude's answer for a standup note: the structured-output type (the SDK makes the JSON schema from it).
// StandupAiService keeps only alertIds that were in the facts.
public record AiNoteDraft(
        String text,
        List<Long> alertIds
) {
}

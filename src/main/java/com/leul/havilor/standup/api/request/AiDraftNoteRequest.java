package com.leul.havilor.standup.api.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// POST /api/v1/standup/notes/ai-draft: the day whose activity the note is about.
public record AiDraftNoteRequest(
        @NotNull LocalDate noteDate
) {
}

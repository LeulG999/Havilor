package com.leul.havilor.standup.api.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

// PATCH /api/v1/standup/notes/{id}. Every field optional (null = unchanged); alertIds replaces the whole list.
public record UpdateNoteRequest(
        LocalDate noteDate,
        @Size(min = 1, max = 10_000) String text,
        @Size(max = 50) List<Long> alertIds
) {
}

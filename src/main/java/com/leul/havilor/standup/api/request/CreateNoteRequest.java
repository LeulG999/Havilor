package com.leul.havilor.standup.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

// POST /api/v1/standup/notes. The author is the logged-in user; alertIds are the alerts the note mentions.
public record CreateNoteRequest(
        @NotNull LocalDate noteDate,
        @NotBlank @Size(max = 10_000) String text,
        @Size(max = 50) List<@NotNull Long> alertIds
) {

    public CreateNoteRequest {
        alertIds = alertIds == null ? List.of() : alertIds;
    }
}

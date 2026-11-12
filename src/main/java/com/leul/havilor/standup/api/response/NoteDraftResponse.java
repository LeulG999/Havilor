package com.leul.havilor.standup.api.response;

import java.util.List;

// An AI-written draft for a standup note. Nothing is saved: the analyst edits it and saves it like any note.
public record NoteDraftResponse(
        String text,
        List<Long> alertIds
) {
}

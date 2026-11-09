package com.leul.havilor.standup.api.response;

import com.leul.havilor.standup.domain.StandupNote;
import com.leul.havilor.standup.domain.enums.NoteStatus;

import java.time.LocalDate;
import java.util.List;

public record NoteResponse(
        Long id,
        Long authorId,
        LocalDate noteDate,
        String text,
        NoteStatus status,
        List<Long> alertIds
) {

    public static NoteResponse from(StandupNote note) {
        return new NoteResponse(note.getId(), note.getAuthorId(), note.getNoteDate(), note.getText(),
                note.getStatus(), note.getAlertIds().stream().sorted().toList());
    }
}

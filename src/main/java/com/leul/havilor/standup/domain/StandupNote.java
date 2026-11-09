package com.leul.havilor.standup.domain;

import com.leul.havilor.common.error.ConflictException;
import com.leul.havilor.standup.domain.enums.NoteStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// An analyst's notes for one day's standup. The alerts it mentions are other module's rows, so they are kept
// as plain ids in the join table standup_note_alert (@ElementCollection): many notes ↔ many alerts.
@Entity
@Table(name = "standup_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StandupNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(nullable = false, length = 10_000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteStatus status;

    @ElementCollection
    @CollectionTable(name = "standup_note_alert", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "alert_id")
    private Set<Long> alertIds = new HashSet<>();

    // A new note is a DRAFT.
    public StandupNote(Long authorId, LocalDate noteDate, String text, Collection<Long> alertIds) {
        this.authorId = authorId;
        this.noteDate = noteDate;
        this.text = text;
        this.alertIds.addAll(alertIds);
        this.status = NoteStatus.DRAFT;
    }

    public void changeNoteDate(LocalDate noteDate) {
        ensureDraft();
        this.noteDate = noteDate;
    }

    public void changeText(String text) {
        ensureDraft();
        this.text = text;
    }

    public void changeAlerts(Collection<Long> alertIds) {
        ensureDraft();
        this.alertIds.clear();
        this.alertIds.addAll(alertIds);
    }

    // Sharing twice is harmless: the note simply stays SHARED.
    public void share() {
        this.status = NoteStatus.SHARED;
    }

    public Set<Long> getAlertIds() {
        return Collections.unmodifiableSet(alertIds);
    }

    private void ensureDraft() {
        if (status == NoteStatus.SHARED) {
            throw new ConflictException("Note " + id + " is shared and can't be changed");
        }
    }
}

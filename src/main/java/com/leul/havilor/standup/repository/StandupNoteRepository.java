package com.leul.havilor.standup.repository;

import com.leul.havilor.standup.domain.StandupNote;
import com.leul.havilor.standup.domain.enums.NoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StandupNoteRepository extends JpaRepository<StandupNote, Long> {

    Page<StandupNote> findByAuthorId(Long authorId, Pageable pageable);

    Page<StandupNote> findByStatus(NoteStatus status, Pageable pageable);

    // Someone else's note is "not found" for you: its existence is not revealed.
    Optional<StandupNote> findByIdAndAuthorId(Long id, Long authorId);
}

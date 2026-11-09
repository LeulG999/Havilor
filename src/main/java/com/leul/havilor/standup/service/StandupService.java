package com.leul.havilor.standup.service;

import com.leul.havilor.alert.service.AlertQueryService;
import com.leul.havilor.common.error.BadRequestException;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import com.leul.havilor.standup.api.request.CreateNoteRequest;
import com.leul.havilor.standup.api.request.UpdateNoteRequest;
import com.leul.havilor.standup.api.response.NoteResponse;
import com.leul.havilor.standup.domain.StandupNote;
import com.leul.havilor.standup.domain.enums.NoteStatus;
import com.leul.havilor.standup.repository.StandupNoteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Standup notes. Everyone works on their own notes only; managers read the SHARED notes of the whole team.
@Service
public class StandupService {

    private static final Sort NEWEST_DAY_FIRST = Sort.by(Sort.Order.desc("noteDate"), Sort.Order.desc("id"));

    private final StandupNoteRepository noteRepository;
    private final AlertQueryService alertQueryService;

    public StandupService(StandupNoteRepository noteRepository, AlertQueryService alertQueryService) {
        this.noteRepository = noteRepository;
        this.alertQueryService = alertQueryService;
    }

    @Transactional
    public NoteResponse create(CreateNoteRequest request, Long authorId) {
        checkAlertsExist(request.alertIds());
        StandupNote note = new StandupNote(authorId, request.noteDate(), request.text().trim(), request.alertIds());
        return NoteResponse.from(noteRepository.save(note));
    }

    // Own note only (someone else's → 404); a SHARED note → 409.
    @Transactional
    public NoteResponse update(Long id, UpdateNoteRequest request, Long authorId) {
        StandupNote note = loadOwn(id, authorId);
        if (request.noteDate() != null) {
            note.changeNoteDate(request.noteDate());
        }
        if (request.text() != null) {
            note.changeText(request.text().trim());
        }
        if (request.alertIds() != null) {
            checkAlertsExist(request.alertIds());
            note.changeAlerts(request.alertIds());
        }
        return NoteResponse.from(note);
    }

    @Transactional
    public NoteResponse share(Long id, Long authorId) {
        StandupNote note = loadOwn(id, authorId);
        note.share();
        return NoteResponse.from(note);
    }

    // My notes (drafts and shared), newest day first.
    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> own(Long authorId, int page, int size) {
        return PageResponse.from(noteRepository.findByAuthorId(authorId, Paging.pageRequest(page, size, NEWEST_DAY_FIRST))
                .map(NoteResponse::from));
    }

    // For managers: everybody's SHARED notes. Drafts are never shown.
    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> team(int page, int size) {
        return PageResponse.from(noteRepository.findByStatus(NoteStatus.SHARED,
                Paging.pageRequest(page, size, NEWEST_DAY_FIRST)).map(NoteResponse::from));
    }

    private StandupNote loadOwn(Long id, Long authorId) {
        return noteRepository.findByIdAndAuthorId(id, authorId).orElseThrow(() -> NotFoundException.of("Note", id));
    }

    // Mentioned alerts must exist: asked through the alert module's service.
    private void checkAlertsExist(List<Long> alertIds) {
        for (Long alertId : alertIds) {
            if (!alertQueryService.exists(alertId)) {
                throw new BadRequestException("alertIds", "no alert with id " + alertId);
            }
        }
    }
}

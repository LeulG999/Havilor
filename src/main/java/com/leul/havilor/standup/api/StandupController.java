package com.leul.havilor.standup.api;

import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import com.leul.havilor.standup.api.request.CreateNoteRequest;
import com.leul.havilor.standup.api.request.UpdateNoteRequest;
import com.leul.havilor.standup.api.response.NoteResponse;
import com.leul.havilor.standup.service.StandupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/standup")
public class StandupController {

    private final StandupService standupService;

    public StandupController(StandupService standupService) {
        this.standupService = standupService;
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody CreateNoteRequest request, @AuthenticationPrincipal Jwt token) {
        return standupService.create(request, userId(token));
    }

    @PatchMapping("/notes/{id}")
    public NoteResponse update(@PathVariable Long id, @Valid @RequestBody UpdateNoteRequest request,
                               @AuthenticationPrincipal Jwt token) {
        return standupService.update(id, request, userId(token));
    }

    @PostMapping("/notes/{id}/share")
    public NoteResponse share(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return standupService.share(id, userId(token));
    }

    // My own notes.
    @GetMapping("/notes")
    public PageResponse<NoteResponse> own(@AuthenticationPrincipal Jwt token,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "" + Paging.DEFAULT_SIZE) int size) {
        return standupService.own(userId(token), page, size);
    }

    // MANAGER only (rule in SecurityConfiguration): the team's shared notes.
    @GetMapping("/team")
    public PageResponse<NoteResponse> team(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "" + Paging.DEFAULT_SIZE) int size) {
        return standupService.team(page, size);
    }

    private static Long userId(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}

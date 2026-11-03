package com.leul.havilor.playbook.api;

import com.leul.havilor.playbook.api.response.PlaybookDetailResponse;
import com.leul.havilor.playbook.api.response.PlaybookSummaryResponse;
import com.leul.havilor.playbook.service.PlaybookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playbooks")
public class PlaybookController {

    private final PlaybookService playbookService;

    public PlaybookController(PlaybookService playbookService) {
        this.playbookService = playbookService;
    }

    @GetMapping
    public List<PlaybookSummaryResponse> list() {
        return playbookService.list();
    }

    @GetMapping("/{id}")
    public PlaybookDetailResponse getById(@PathVariable Long id) {
        return playbookService.getById(id);
    }
}

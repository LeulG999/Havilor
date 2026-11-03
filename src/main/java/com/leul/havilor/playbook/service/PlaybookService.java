package com.leul.havilor.playbook.service;

import com.leul.havilor.common.CaseType;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.playbook.api.response.PlaybookDetailResponse;
import com.leul.havilor.playbook.api.response.PlaybookStepResponse;
import com.leul.havilor.playbook.api.response.PlaybookSummaryResponse;
import com.leul.havilor.playbook.domain.PlaybookStep;
import com.leul.havilor.playbook.repository.PlaybookRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// The playbook module's public face. Other modules call this, never the repository or the entities,
// and get DTOs back, so the playbook tables can change without touching them.
@Service
public class PlaybookService {

    private static final Sort BY_CASE_TYPE_THEN_VERSION = Sort.by("caseType", "version");

    private final PlaybookRepository playbookRepository;

    public PlaybookService(PlaybookRepository playbookRepository) {
        this.playbookRepository = playbookRepository;
    }

    // Not paged: playbooks are a handful of seeded rows, one per case type and version.
    @Transactional(readOnly = true)
    public List<PlaybookSummaryResponse> list() {
        return playbookRepository.findAll(BY_CASE_TYPE_THEN_VERSION).stream()
                .map(PlaybookSummaryResponse::from)
                .toList();
    }

    // For the alert module: the playbook a new alert of this case type should follow, if there is one.
    @Transactional(readOnly = true)
    public Optional<ActivePlaybook> findActiveFor(CaseType caseType) {
        return playbookRepository.findActiveWithStepsByCaseType(caseType)
                .map(p -> new ActivePlaybook(p.getId(), p.getVersion(),
                        p.getSteps().stream().map(PlaybookStep::getId).toList()));
    }

    // For the alert module: the steps of one playbook, in order (1 query).
    @Transactional(readOnly = true)
    public List<PlaybookStepResponse> getSteps(Long playbookId) {
        return getById(playbookId).steps();
    }

    @Transactional(readOnly = true)
    public PlaybookDetailResponse getById(Long id) {
        return playbookRepository.findWithStepsById(id)
                .map(PlaybookDetailResponse::from)
                .orElseThrow(() -> NotFoundException.of("Playbook", id));
    }
}

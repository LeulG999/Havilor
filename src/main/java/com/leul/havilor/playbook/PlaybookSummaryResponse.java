package com.leul.havilor.playbook;

import com.leul.havilor.common.CaseType;

// One row of GET /api/v1/playbooks (no steps).
public record PlaybookSummaryResponse(Long id, String name, CaseType caseType, int version, boolean active) {

    static PlaybookSummaryResponse from(Playbook playbook) {
        return new PlaybookSummaryResponse(playbook.getId(), playbook.getName(), playbook.getCaseType(),
                playbook.getVersion(), playbook.isActive());
    }
}

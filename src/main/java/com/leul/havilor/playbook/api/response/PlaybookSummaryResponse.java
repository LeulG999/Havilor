package com.leul.havilor.playbook.api.response;

import com.leul.havilor.common.CaseType;
import com.leul.havilor.playbook.domain.Playbook;

import java.io.Serializable;


// One row of GET /api/v1/playbooks (no steps).
public record PlaybookSummaryResponse(Long id, String name, CaseType caseType, int version, boolean active) implements Serializable {

    public static PlaybookSummaryResponse from(Playbook playbook) {
        return new PlaybookSummaryResponse(playbook.getId(), playbook.getName(), playbook.getCaseType(),
                playbook.getVersion(), playbook.isActive());
    }
}

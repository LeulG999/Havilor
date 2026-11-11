package com.leul.havilor.playbook.api.response;

import com.leul.havilor.common.CaseType;
import com.leul.havilor.playbook.domain.Playbook;

import java.io.Serializable;
import java.util.List;

// GET /api/v1/playbooks/{id}: the playbook with its steps in order.
public record PlaybookDetailResponse(
        Long id,
        String name,
        CaseType caseType,
        int version,
        boolean active,
        List<PlaybookStepResponse> steps
) implements Serializable {

    public static PlaybookDetailResponse from(Playbook playbook) {
        return new PlaybookDetailResponse(playbook.getId(), playbook.getName(), playbook.getCaseType(),
                playbook.getVersion(), playbook.isActive(),
                playbook.getSteps().stream().map(PlaybookStepResponse::from).toList());
    }
}

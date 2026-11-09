package com.leul.havilor.escalation.api.response;

import com.leul.havilor.escalation.domain.Escalation;
import com.leul.havilor.escalation.domain.enums.EscalationOutcome;
import com.leul.havilor.escalation.domain.enums.ExternalStatus;

public record EscalationResponse(
        Long id,
        Long alertId,
        String team,
        String reason,
        ExternalStatus externalStatus,
        EscalationOutcome outcome
) {

    public static EscalationResponse from(Escalation escalation) {
        return new EscalationResponse(escalation.getId(), escalation.getAlertId(), escalation.getTeam(),
                escalation.getReason(), escalation.getExternalStatus(), escalation.getOutcome());
    }
}

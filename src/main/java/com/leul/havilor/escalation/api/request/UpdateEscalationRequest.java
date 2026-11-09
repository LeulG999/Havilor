package com.leul.havilor.escalation.api.request;

import com.leul.havilor.escalation.domain.enums.EscalationOutcome;
import com.leul.havilor.escalation.domain.enums.ExternalStatus;

// PATCH /api/v1/escalations/{id}. Both optional: null means "don't change it".
// Sending an outcome sends the alert back to INVESTIGATING.
public record UpdateEscalationRequest(
        ExternalStatus externalStatus,
        EscalationOutcome outcome
) {
}

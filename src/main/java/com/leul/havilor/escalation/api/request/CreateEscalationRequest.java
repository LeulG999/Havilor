package com.leul.havilor.escalation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /api/v1/alerts/{alertId}/escalations
public record CreateEscalationRequest(
        @NotBlank @Size(max = 100) String team,
        @NotBlank @Size(max = 2000) String reason
) {
}

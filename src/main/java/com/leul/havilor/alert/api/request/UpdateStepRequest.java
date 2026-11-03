package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.StepStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// PATCH /api/v1/alerts/{id}/steps/{stepId}. answer is free text: the step's answerOptions are suggestions,
// and an analyst often adds a note ("Clean, domain registered in 2009").
public record UpdateStepRequest(
        @NotNull StepStatus status,
        @Size(max = 2000) String answer
) {
}

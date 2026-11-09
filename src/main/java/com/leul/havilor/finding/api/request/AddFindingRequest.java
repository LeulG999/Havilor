package com.leul.havilor.finding.api.request;

import com.leul.havilor.finding.domain.enums.FindingType;
import com.leul.havilor.finding.domain.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// POST /api/v1/alerts/{alertId}/findings: something the analyst found herself. Source is always MANUAL.
public record AddFindingRequest(
        @NotNull FindingType type,
        @NotNull Severity severity,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 10_000) String teachingText
) {
}

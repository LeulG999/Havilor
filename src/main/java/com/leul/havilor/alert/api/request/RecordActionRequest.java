package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.enums.ActionResult;
import com.leul.havilor.alert.domain.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// POST /api/v1/alerts/{alertId}/actions. Who and when come from the token and the clock, not from the body.
public record RecordActionRequest(
        @NotNull ActionType type,
        @NotNull ActionResult result,
        @Size(max = 2000) String note
) {
}

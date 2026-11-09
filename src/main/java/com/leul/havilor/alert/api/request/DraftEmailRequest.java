package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.enums.EmailDraftType;
import jakarta.validation.constraints.NotNull;

// POST /api/v1/alerts/{alertId}/ai/draft-email
public record DraftEmailRequest(@NotNull EmailDraftType type) {
}

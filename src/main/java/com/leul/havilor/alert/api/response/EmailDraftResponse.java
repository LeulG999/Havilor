package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.enums.EmailDraftType;

// A draft only: not saved, not sent. The analyst copies, edits and sends it herself.
public record EmailDraftResponse(EmailDraftType type, String draft) {
}

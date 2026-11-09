package com.leul.havilor.audit.api.response;

import com.leul.havilor.audit.domain.AuditEntry;

import java.time.Instant;

public record AuditEntryResponse(
        Long id,
        Long actorId,
        String action,
        String entityType,
        Long entityId,
        String oldValue,
        String newValue,
        Instant createdAt
) {

    public static AuditEntryResponse from(AuditEntry entry) {
        return new AuditEntryResponse(entry.getId(), entry.getActorId(), entry.getAction(), entry.getEntityType(),
                entry.getEntityId(), entry.getOldValue(), entry.getNewValue(), entry.getCreatedAt());
    }
}

package com.leul.havilor.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

// One line of the audit log. @Immutable: Hibernate never writes an UPDATE for it (and the database trigger
// refuses UPDATE and DELETE anyway).
@Entity
@Immutable
@Table(name = "audit_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who did it.
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditEntry(Long actorId, String action, String entityType, Long entityId, String oldValue,
                      String newValue, Instant createdAt) {
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = createdAt;
    }
}

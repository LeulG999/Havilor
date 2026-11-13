package com.leul.havilor.audit.service;

import com.leul.havilor.audit.api.response.AuditEntryResponse;
import com.leul.havilor.audit.domain.AuditEntry;
import com.leul.havilor.audit.repository.AuditEntryRepository;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

// The audit log. Services that change data call record(...) explicitly, inside their own transaction:
// if the change rolls back, so does its audit entry. This module depends on no other module.
@Service
public class AuditService {

    // Everything that happens to an alert (and its steps, actions, findings, escalations) is logged against the
    // alert, so one query gives the alert's whole history.
    public static final String ALERT = "ALERT";

    // Account changes (Flow F30): created, deactivated / reactivated, role changed. entityId = the user's id.
    public static final String USER = "USER";

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("id"));

    private final AuditEntryRepository auditEntryRepository;
    private final Clock clock;

    public AuditService(AuditEntryRepository auditEntryRepository, Clock clock) {
        this.auditEntryRepository = auditEntryRepository;
        this.clock = clock;
    }

    @Transactional
    public void record(Long actorId, String action, String entityType, Long entityId, String oldValue,
                       String newValue) {
        auditEntryRepository.save(
                new AuditEntry(actorId, action, entityType, entityId, oldValue, newValue, clock.instant()));
    }

    // One entity's history, oldest first (reads like a story). An id with no entries gives [].
    @Transactional(readOnly = true)
    public List<AuditEntryResponse> forEntity(String entityType, Long entityId) {
        return auditEntryRepository.findByEntityTypeAndEntityIdOrderById(entityType, entityId).stream()
                .map(AuditEntryResponse::from).toList();
    }

    // What one person did in a time range [from, to), oldest first (e.g. one day, for an AI-drafted standup note).
    @Transactional(readOnly = true)
    public List<AuditEntryResponse> byActorBetween(Long actorId, Instant from, Instant to) {
        return auditEntryRepository.findByActorIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderById(actorId, from, to)
                .stream().map(AuditEntryResponse::from).toList();
    }

    // The whole log for managers, newest first.
    @Transactional(readOnly = true)
    public PageResponse<AuditEntryResponse> all(int page, int size) {
        return PageResponse.from(auditEntryRepository.findAll(Paging.pageRequest(page, size, NEWEST_FIRST))
                .map(AuditEntryResponse::from));
    }
}

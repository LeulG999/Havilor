package com.leul.havilor.audit.repository;

import com.leul.havilor.audit.domain.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;

// A plain Repository, not JpaRepository: it offers save and reads only, no delete. Append-only by design.
public interface AuditEntryRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    List<AuditEntry> findByEntityTypeAndEntityIdOrderById(String entityType, Long entityId);

    Page<AuditEntry> findAll(Pageable pageable);
}

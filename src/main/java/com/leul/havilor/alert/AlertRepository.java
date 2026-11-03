package com.leul.havilor.alert;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByTicketNumber(String ticketNumber);

    // Only the id: the duplicate check doesn't need the whole alert.
    @Query("select a.id from Alert a where a.ticketNumber = :ticketNumber")
    Optional<Long> findIdByTicketNumber(String ticketNumber);

    // The detail view needs the alert plus three child lists. Fetch-joining all three in ONE query is not
    // possible with Lists (Hibernate's MultipleBagFetchException), and would return
    // recipients × urls × attachments rows anyway. So: three small queries, each fetching one list.
    // Run them in one transaction; they fill the same Alert instance in the persistence context.
    @Query("select a from Alert a left join fetch a.recipients where a.id = :id")
    Optional<Alert> findWithRecipientsById(Long id);

    @Query("select a from Alert a left join fetch a.urls where a.id = :id")
    Optional<Alert> findWithUrlsById(Long id);

    @Query("select a from Alert a left join fetch a.attachments where a.id = :id")
    Optional<Alert> findWithAttachmentsById(Long id);

    // For reading steps: the alert and its step progress, in one query.
    @Query("select a from Alert a left join fetch a.stepProgress where a.id = :id")
    Optional<Alert> findWithStepProgressById(Long id);

    // For changing steps and for closing. OPTIMISTIC_FORCE_INCREMENT bumps the alert's @Version at commit
    // even if no alert column changed. So "tick a step" and "close the alert" can't both succeed from the
    // same starting point: the second one to commit gets an optimistic-lock failure (409).
    // Without it, a step set back to PENDING while the alert is being closed could leave a closed alert
    // with a pending step.
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select a from Alert a left join fetch a.stepProgress where a.id = :id")
    Optional<Alert> findWithStepProgressForChangeById(Long id);
}

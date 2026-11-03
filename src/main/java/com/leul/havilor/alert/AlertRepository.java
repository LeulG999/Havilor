package com.leul.havilor.alert;

import org.springframework.data.jpa.repository.JpaRepository;
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

    // For the steps endpoints and closing: the alert and its step progress, in one query.
    @Query("select a from Alert a left join fetch a.stepProgress where a.id = :id")
    Optional<Alert> findWithStepProgressById(Long id);
}

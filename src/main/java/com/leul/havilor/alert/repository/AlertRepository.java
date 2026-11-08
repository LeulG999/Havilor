package com.leul.havilor.alert.repository;

import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Only the id: the duplicate check doesn't need the whole alert.
    @Query("select a.id from Alert a where a.ticketNumber = :ticketNumber")
    Optional<Long> findIdByTicketNumber(String ticketNumber);

    // Every click of the alert, with its recipient and URL, for the detail view.
    @Query("""
            select c from ClickEvent c join fetch c.recipient join fetch c.url u
            where u.alert.id = :alertId order by c.clickedAt, c.id""")
    List<ClickEvent> findClicksByAlertId(Long alertId);

    // For the steps and for closing: the alert and its step progress, in one query.
    @Query("select a from Alert a left join fetch a.stepProgress where a.id = :id")
    Optional<Alert> findWithStepProgressById(Long id);
}

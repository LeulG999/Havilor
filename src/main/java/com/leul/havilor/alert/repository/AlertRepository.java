package com.leul.havilor.alert.repository;

import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.ClickEvent;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Only the id: the duplicate check doesn't need the whole alert.
    @Query("select a.id from Alert a where a.ticketNumber = :ticketNumber")
    Optional<Long> findIdByTicketNumber(String ticketNumber);

    // For numbering manual tickets: how many MAN-<date>- tickets exist already.
    long countByTicketNumberStartingWith(String prefix);

    // ---- Dashboard (Flow F13): Spring Data turns each method name into one "SELECT COUNT(*) ... WHERE ..." ----

    long countByStatusNot(AlertStatus status);

    long countByStatusNotAndAssigneeId(AlertStatus status, Long assigneeId);

    long countByStatusNotAndPriority(AlertStatus status, Priority priority);

    long countByClosedAtGreaterThanEqual(Instant from);

    // Team dashboard (Flow F26).
    long countByStatusNotAndAssigneeIdIsNull(AlertStatus status);

    long countByClosedAtGreaterThanEqualAndAssigneeId(Instant from, Long assigneeId);

    Optional<Alert> findFirstByStatusNotOrderByReceivedAtAscIdAsc(AlertStatus status);

    // Alerts received in [from, to): one day of the "alerts per day" chart.
    long countByReceivedAtGreaterThanEqualAndReceivedAtLessThan(Instant from, Instant to);

    // The 5 newest alerts (same order as the queue).
    List<Alert> findTop5ByOrderByReceivedAtDescIdDesc();

    // Every click of the alert, with its recipient and URL, for the detail view.
    @Query("""
            select c from ClickEvent c join fetch c.recipient join fetch c.url u
            where u.alert.id = :alertId order by c.clickedAt, c.id""")
    List<ClickEvent> findClicksByAlertId(Long alertId);

    // For the steps and for closing: the alert and its step progress, in one query.
    @Query("select a from Alert a left join fetch a.stepProgress where a.id = :id")
    Optional<Alert> findWithStepProgressById(Long id);
}

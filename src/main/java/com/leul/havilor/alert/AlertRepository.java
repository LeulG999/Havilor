package com.leul.havilor.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Used by the idempotency check: is this source ticket already an alert?
    Optional<Alert> findByTicketNumber(String ticketNumber);
}

package com.leul.havilor.escalation.repository;

import com.leul.havilor.escalation.domain.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    List<Escalation> findByAlertIdOrderById(Long alertId);
}

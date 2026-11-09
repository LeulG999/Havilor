package com.leul.havilor.finding.repository;

import com.leul.havilor.finding.domain.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByAlertIdOrderById(Long alertId);
}

package com.leul.havilor.alert.repository;

import com.leul.havilor.alert.domain.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {

    List<Action> findByAlertIdOrderById(Long alertId);
}

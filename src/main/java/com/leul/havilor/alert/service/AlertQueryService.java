package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The read side of an alert: load it and turn it into the detail DTO.
// The transaction keeps the session open while the lazy child lists load.
@Service
public class AlertQueryService {

    private final AlertRepository alertRepository;

    public AlertQueryService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public AlertDetailResponse getById(Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> NotFoundException.of("Alert", id));
        return AlertDetailResponse.from(alert, alertRepository.findClicksByAlertId(id));
    }
}

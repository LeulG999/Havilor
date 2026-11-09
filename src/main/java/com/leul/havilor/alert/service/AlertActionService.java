package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.request.RecordActionRequest;
import com.leul.havilor.alert.api.response.ActionResponse;
import com.leul.havilor.alert.domain.Action;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.repository.ActionRepository;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

// What the analyst did about an alert: block a sender, purge the email, reset a password...
@Service
public class AlertActionService {

    private final AlertRepository alertRepository;
    private final ActionRepository actionRepository;
    private final AuditService auditService;
    private final Clock clock;

    public AlertActionService(AlertRepository alertRepository, ActionRepository actionRepository,
                              AuditService auditService, Clock clock) {
        this.alertRepository = alertRepository;
        this.actionRepository = actionRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    // Unknown alert → 404; closed alert → 409 (a closed case is final, like every other change).
    @Transactional
    public ActionResponse record(Long alertId, RecordActionRequest request, Long performedBy) {
        Alert alert = load(alertId);
        alert.ensureOpen();
        Action action = new Action(alertId, request.type(), request.result(), blankToNull(request.note()),
                performedBy, clock.instant());
        Action saved = actionRepository.save(action);
        auditService.record(performedBy, "ACTION_RECORDED", AuditService.ALERT, alertId, null,
                "action " + saved.getId() + ": " + saved.getType() + " " + saved.getResult()
                        + (saved.getNote() == null ? "" : ", note=" + saved.getNote()));
        return ActionResponse.from(saved);
    }

    // Oldest first. Reading is allowed on a closed alert too.
    @Transactional(readOnly = true)
    public List<ActionResponse> list(Long alertId) {
        load(alertId);
        return actionRepository.findByAlertIdOrderById(alertId).stream().map(ActionResponse::from).toList();
    }

    private Alert load(Long alertId) {
        return alertRepository.findById(alertId).orElseThrow(() -> NotFoundException.of("Alert", alertId));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

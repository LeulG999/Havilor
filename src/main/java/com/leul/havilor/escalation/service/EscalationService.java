package com.leul.havilor.escalation.service;

import com.leul.havilor.alert.service.AlertQueryService;
import com.leul.havilor.alert.service.AlertService;
import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.escalation.api.request.CreateEscalationRequest;
import com.leul.havilor.escalation.api.request.UpdateEscalationRequest;
import com.leul.havilor.escalation.api.response.EscalationResponse;
import com.leul.havilor.escalation.domain.Escalation;
import com.leul.havilor.escalation.repository.EscalationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Handing an alert to another team, and getting the answer back. The alert's status is changed through the
// alert module's service (escalation → alert; the alert module never calls this one).
@Service
public class EscalationService {

    private final EscalationRepository escalationRepository;
    private final AlertService alertService;
    private final AlertQueryService alertQueryService;
    private final AuditService auditService;

    public EscalationService(EscalationRepository escalationRepository, AlertService alertService,
                             AlertQueryService alertQueryService, AuditService auditService) {
        this.escalationRepository = escalationRepository;
        this.alertService = alertService;
        this.alertQueryService = alertQueryService;
        this.auditService = auditService;
    }

    // One transaction: the alert becomes ESCALATED and the escalation is saved, or neither happens.
    // Unknown alert → 404; closed alert → 409.
    @Transactional
    public EscalationResponse create(Long alertId, CreateEscalationRequest request, Long actorId) {
        alertService.escalate(alertId);
        Escalation saved = escalationRepository.save(
                new Escalation(alertId, request.team().trim(), request.reason().trim()));
        auditService.record(actorId, "ESCALATED", AuditService.ALERT, alertId, null,
                "escalation " + saved.getId() + ": team=" + saved.getTeam() + ", reason=" + saved.getReason());
        return EscalationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EscalationResponse> listForAlert(Long alertId) {
        alertQueryService.ensureExists(alertId);
        return escalationRepository.findByAlertIdOrderById(alertId).stream().map(EscalationResponse::from).toList();
    }

    // When the other team's outcome arrives, the alert goes back to INVESTIGATING (if it is still ESCALATED).
    @Transactional
    public EscalationResponse update(Long id, UpdateEscalationRequest request, Long actorId) {
        Escalation escalation = escalationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Escalation", id));
        String before = describe(escalation);
        if (request.externalStatus() != null) {
            escalation.changeExternalStatus(request.externalStatus());
        }
        if (request.outcome() != null) {
            escalation.recordOutcome(request.outcome());
            alertService.endEscalation(escalation.getAlertId());
        }
        auditService.record(actorId, "ESCALATION_UPDATED", AuditService.ALERT, escalation.getAlertId(), before,
                describe(escalation));
        return EscalationResponse.from(escalation);
    }

    private static String describe(Escalation escalation) {
        return "escalation " + escalation.getId() + ": externalStatus=" + escalation.getExternalStatus()
                + ", outcome=" + escalation.getOutcome();
    }
}

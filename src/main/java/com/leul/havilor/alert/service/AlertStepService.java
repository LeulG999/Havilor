package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.request.UpdateStepRequest;
import com.leul.havilor.alert.api.response.AlertStepResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.StepProgress;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.playbook.api.response.PlaybookStepResponse;
import com.leul.havilor.playbook.service.PlaybookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// The steps of an alert's playbook. The step texts come from the playbook module (through its service);
// the progress comes from our own step_progress rows. They are joined here, in Java, by step id.
@Service
public class AlertStepService {

    private final AlertRepository alertRepository;
    private final PlaybookService playbookService;
    private final AuditService auditService;
    private final Clock clock;

    public AlertStepService(AlertRepository alertRepository, PlaybookService playbookService,
                            AuditService auditService, Clock clock) {
        this.alertRepository = alertRepository;
        this.playbookService = playbookService;
        this.auditService = auditService;
        this.clock = clock;
    }

    // 2 queries: the alert with its progress rows, then the playbook with its steps.
    @Transactional(readOnly = true)
    public List<AlertStepResponse> getSteps(Long alertId) {
        Alert alert = loadWithProgress(alertId);
        if (alert.getPlaybookId() == null) {
            return List.of();
        }
        Map<Long, StepProgress> progressByStep = alert.getStepProgress().stream()
                .collect(Collectors.toMap(StepProgress::getPlaybookStepId, Function.identity()));
        // Playbook order, not progress-row order.
        return playbookService.getSteps(alert.getPlaybookId()).stream()
                .filter(step -> progressByStep.containsKey(step.id()))
                .map(step -> AlertStepResponse.of(step, progressByStep.get(step.id())))
                .toList();
    }

    // Closed alert → 409 (Alert.ensureOpen); step not on this alert → 404.
    @Transactional
    public AlertStepResponse updateStep(Long alertId, Long stepId, UpdateStepRequest request, Long actorId) {
        Alert alert = alertRepository.findWithStepProgressById(alertId)
                .orElseThrow(() -> NotFoundException.of("Alert", alertId));
        String before = alert.getStepProgress().stream()
                .filter(p -> p.getPlaybookStepId().equals(stepId))
                .findFirst().map(AlertStepService::describe).orElse(null);
        StepProgress progress = alert.recordStep(stepId, request.status(), request.answer(), clock);
        PlaybookStepResponse step = playbookService.getSteps(alert.getPlaybookId()).stream()
                .filter(s -> s.id().equals(stepId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("Playbook step", stepId));
        alertRepository.flush();
        auditService.record(actorId, "STEP_UPDATED", AuditService.ALERT, alertId, before, describe(progress));
        return AlertStepResponse.of(step, progress);
    }

    private static String describe(StepProgress progress) {
        return "step " + progress.getPlaybookStepId() + ": " + progress.getStatus()
                + (progress.getAnswer() == null ? "" : ", answer=" + progress.getAnswer());
    }

    private Alert loadWithProgress(Long alertId) {
        return alertRepository.findWithStepProgressById(alertId)
                .orElseThrow(() -> NotFoundException.of("Alert", alertId));
    }
}

package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.request.UpdateStepRequest;
import com.leul.havilor.alert.api.response.AlertStepResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.StepProgress;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.playbook.api.response.PlaybookStepResponse;
import com.leul.havilor.playbook.service.PlaybookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AlertStepService(AlertRepository alertRepository, PlaybookService playbookService) {
        this.alertRepository = alertRepository;
        this.playbookService = playbookService;
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
    public AlertStepResponse updateStep(Long alertId, Long stepId, UpdateStepRequest request) {
        Alert alert = alertRepository.findForChangeWithStepProgressById(alertId)
                .orElseThrow(() -> NotFoundException.of("Alert", alertId));
        StepProgress progress = alert.recordStep(stepId, request.status(), request.answer());
        PlaybookStepResponse step = playbookService.getSteps(alert.getPlaybookId()).stream()
                .filter(s -> s.id().equals(stepId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("Playbook step", stepId));
        alertRepository.flush();
        return AlertStepResponse.of(step, progress);
    }

    private Alert loadWithProgress(Long alertId) {
        return alertRepository.findWithStepProgressById(alertId)
                .orElseThrow(() -> NotFoundException.of("Alert", alertId));
    }
}

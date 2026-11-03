package com.leul.havilor.alert.api;

import com.leul.havilor.alert.api.request.UpdateStepRequest;
import com.leul.havilor.alert.api.response.AlertStepResponse;
import com.leul.havilor.alert.service.AlertStepService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/{alertId}/steps")
public class AlertStepController {

    private final AlertStepService alertStepService;

    public AlertStepController(AlertStepService alertStepService) {
        this.alertStepService = alertStepService;
    }

    // The alert's playbook steps in order, each with why it matters and this alert's progress.
    @GetMapping
    public List<AlertStepResponse> getSteps(@PathVariable Long alertId) {
        return alertStepService.getSteps(alertId);
    }

    @PatchMapping("/{stepId}")
    public AlertStepResponse updateStep(@PathVariable Long alertId, @PathVariable Long stepId,
                                        @Valid @RequestBody UpdateStepRequest request) {
        return alertStepService.updateStep(alertId, stepId, request);
    }
}

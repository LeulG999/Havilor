package com.leul.havilor.escalation.api;

import com.leul.havilor.escalation.api.request.CreateEscalationRequest;
import com.leul.havilor.escalation.api.request.UpdateEscalationRequest;
import com.leul.havilor.escalation.api.response.EscalationResponse;
import com.leul.havilor.escalation.service.EscalationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    // The alert becomes ESCALATED. 404 unknown alert, 409 closed alert.
    @PostMapping("/alerts/{alertId}/escalations")
    @ResponseStatus(HttpStatus.CREATED)
    public EscalationResponse create(@PathVariable Long alertId, @Valid @RequestBody CreateEscalationRequest request) {
        return escalationService.create(alertId, request);
    }

    @GetMapping("/alerts/{alertId}/escalations")
    public List<EscalationResponse> list(@PathVariable Long alertId) {
        return escalationService.listForAlert(alertId);
    }

    // The other team's progress or answer. An outcome sends the alert back to INVESTIGATING.
    @PatchMapping("/escalations/{id}")
    public EscalationResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEscalationRequest request) {
        return escalationService.update(id, request);
    }
}

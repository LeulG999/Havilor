package com.leul.havilor.alert.api;

import com.leul.havilor.alert.api.request.RecordActionRequest;
import com.leul.havilor.alert.api.response.ActionResponse;
import com.leul.havilor.alert.service.AlertActionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/{alertId}/actions")
public class AlertActionController {

    private final AlertActionService alertActionService;

    public AlertActionController(AlertActionService alertActionService) {
        this.alertActionService = alertActionService;
    }

    // performedBy = the logged-in user (the token's subject).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActionResponse record(@PathVariable Long alertId, @Valid @RequestBody RecordActionRequest request,
                                 @AuthenticationPrincipal Jwt token) {
        return alertActionService.record(alertId, request, Long.valueOf(token.getSubject()));
    }

    @GetMapping
    public List<ActionResponse> list(@PathVariable Long alertId) {
        return alertActionService.list(alertId);
    }
}

package com.leul.havilor.alert.api;

import com.leul.havilor.alert.api.request.DraftEmailRequest;
import com.leul.havilor.alert.api.response.AiAssessmentResponse;
import com.leul.havilor.alert.api.response.EmailDraftResponse;
import com.leul.havilor.alert.service.AlertAiService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The AI assistant (Claude). 502 when it can't answer (no key, API down, declined).
@RestController
@RequestMapping("/api/v1/alerts/{alertId}/ai")
public class AlertAiController {

    private final AlertAiService alertAiService;

    public AlertAiController(AlertAiService alertAiService) {
        this.alertAiService = alertAiService;
    }

    // Summary + suggested verdict + reason; the suggestion is saved on the alert. The analyst still decides.
    @PostMapping("/assessment")
    public AiAssessmentResponse assess(@PathVariable Long alertId, @AuthenticationPrincipal Jwt token) {
        return alertAiService.assess(alertId, Long.valueOf(token.getSubject()));
    }

    // A draft email (not saved, not sent).
    @PostMapping("/draft-email")
    public EmailDraftResponse draftEmail(@PathVariable Long alertId, @Valid @RequestBody DraftEmailRequest request) {
        return alertAiService.draftEmail(alertId, request.type());
    }
}

package com.leul.havilor.finding.api;

import com.leul.havilor.finding.api.request.AddFindingRequest;
import com.leul.havilor.finding.api.response.FindingResponse;
import com.leul.havilor.finding.service.FindingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// The findings of one alert. The URL sits under /alerts, but the data belongs to the finding module.
@RestController
@RequestMapping("/api/v1/alerts/{alertId}/findings")
public class FindingController {

    private final FindingService findingService;

    public FindingController(FindingService findingService) {
        this.findingService = findingService;
    }

    // Automatic and manual findings, oldest first.
    @GetMapping
    public List<FindingResponse> list(@PathVariable Long alertId) {
        return findingService.listForAlert(alertId);
    }

    // The analyst adds what she found herself (source MANUAL).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FindingResponse add(@PathVariable Long alertId, @Valid @RequestBody AddFindingRequest request) {
        return findingService.addManual(alertId, request);
    }
}

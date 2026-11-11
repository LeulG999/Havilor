package com.leul.havilor.alert.api;

import com.leul.havilor.alert.api.request.CloseAlertRequest;
import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.api.request.UpdateAlertRequest;
import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.api.response.AlertSummaryResponse;
import com.leul.havilor.alert.api.response.DashboardResponse;
import com.leul.havilor.alert.service.AlertQueryService;
import com.leul.havilor.alert.service.AlertService;
import com.leul.havilor.alert.service.CreateAlertResult;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;
    private final AlertQueryService alertQueryService;

    public AlertController(AlertService alertService, AlertQueryService alertQueryService) {
        this.alertService = alertService;
        this.alertQueryService = alertQueryService;
    }

    // 201 Created for a new ticket, 200 OK with the stored alert for a ticket we already have.
    // Both point Location at the alert.
    @PostMapping
    public ResponseEntity<AlertDetailResponse> create(@Valid @RequestBody CreateAlertRequest request) {
        CreateAlertResult result = alertService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.alert().id())
                .toUri();
        HttpStatus status = result.wasCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).location(location).body(result.alert());
    }

    // Newest first. Out-of-range page/size are corrected by the service (max size 100).
    @GetMapping
    public PageResponse<AlertSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Paging.DEFAULT_SIZE) int size) {
        return alertService.list(page, size);
    }

    // The analyst's start page. "/dashboard" doesn't clash with "/{id}": Spring prefers the exact path.
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal Jwt token) {
        return alertQueryService.dashboard(Long.valueOf(token.getSubject()));
    }

    // An unknown id throws NotFoundException in the service; GlobalExceptionHandler makes it a 404.
    @GetMapping("/{id}")
    public AlertDetailResponse getById(@PathVariable Long id) {
        return alertQueryService.getById(id);
    }

    // The analyst's final decision. 400: no verdict / UNDECIDED; 409: already closed or steps still pending.
    @PostMapping("/{id}/close")
    public AlertDetailResponse close(@PathVariable Long id, @Valid @RequestBody CloseAlertRequest request,
                                     @AuthenticationPrincipal Jwt token) {
        return alertService.close(id, request, Long.valueOf(token.getSubject()));
    }

    // Partial update: only the fields that are sent change. Closed alert or a concurrent change → 409.
    @PatchMapping("/{id}")
    public AlertDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAlertRequest request,
                                      @AuthenticationPrincipal Jwt token) {
        return alertService.update(id, request, Long.valueOf(token.getSubject()));
    }
}

package com.leul.havilor.alert;

import com.leul.havilor.common.PageResponse;
import com.leul.havilor.common.Paging;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
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

    // An unknown id throws NotFoundException in the service; GlobalExceptionHandler makes it a 404.
    @GetMapping("/{id}")
    public AlertDetailResponse getById(@PathVariable Long id) {
        return alertService.getById(id);
    }
}

package com.leul.havilor.audit.api;

import com.leul.havilor.audit.api.response.AuditEntryResponse;
import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // An alert's history, oldest first. Any logged-in user.
    @GetMapping("/alerts/{alertId}/audit")
    public List<AuditEntryResponse> forAlert(@PathVariable Long alertId) {
        return auditService.forEntity(AuditService.ALERT, alertId);
    }

    // The whole log, newest first. MANAGER only (the rule is in SecurityConfiguration).
    @GetMapping("/audit")
    public PageResponse<AuditEntryResponse> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + Paging.DEFAULT_SIZE) int size) {
        return auditService.all(page, size);
    }
}

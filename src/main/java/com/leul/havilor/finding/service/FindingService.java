package com.leul.havilor.finding.service;

import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.finding.api.request.AddFindingRequest;
import com.leul.havilor.finding.api.response.FindingResponse;
import com.leul.havilor.finding.domain.Finding;
import com.leul.havilor.finding.domain.enums.FindingSource;
import com.leul.havilor.finding.repository.FindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class FindingService {

    private final FindingRepository findingRepository;
    private final AuditService auditService;
    private final Clock clock;

    public FindingService(FindingRepository findingRepository, AuditService auditService, Clock clock) {
        this.findingRepository = findingRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    // Called by the alert module while it creates an alert (same transaction): run the rules, save each hit.
    @Transactional
    public List<FindingResponse> runAutomaticChecks(Long alertId, CheckFacts facts) {
        return findingRepository.saveAll(AutomaticChecks.run(alertId, facts, clock.instant())).stream()
                .map(FindingResponse::from)
                .toList();
    }

    // Oldest first. An unknown alert simply has no findings: this module can't look alerts up
    // (that would make alert ↔ finding a cycle).
    @Transactional(readOnly = true)
    public List<FindingResponse> listForAlert(Long alertId) {
        return findingRepository.findByAlertIdOrderById(alertId).stream().map(FindingResponse::from).toList();
    }

    // An unknown alert id breaks the foreign key: GlobalExceptionHandler answers 409.
    @Transactional
    public FindingResponse addManual(Long alertId, AddFindingRequest request, Long actorId) {
        Finding finding = findingRepository.saveAndFlush(new Finding(alertId, request.type(), request.severity(),
                request.title().trim(), request.teachingText(), FindingSource.MANUAL, clock.instant()));
        auditService.record(actorId, "FINDING_ADDED", AuditService.ALERT, alertId, null,
                "finding " + finding.getId() + ": " + finding.getType() + " " + finding.getSeverity()
                        + ", title=" + finding.getTitle());
        return FindingResponse.from(finding);
    }
}

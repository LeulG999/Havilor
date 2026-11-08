package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.request.CloseAlertRequest;
import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.api.request.UpdateAlertRequest;
import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.api.response.AlertSummaryResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.common.error.ConflictException;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import com.leul.havilor.playbook.service.PlaybookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class AlertService {

    // Newest first; id breaks ties so alerts with the same receivedAt never jump between pages.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id"));

    private final AlertRepository alertRepository;
    private final PlaybookService playbookService;
    private final AlertFactory alertFactory;
    private final AlertQueryService alertQueryService;
    private final Clock clock;

    public AlertService(AlertRepository alertRepository, PlaybookService playbookService,
                        AlertFactory alertFactory, AlertQueryService alertQueryService, Clock clock) {
        this.alertRepository = alertRepository;
        this.playbookService = playbookService;
        this.alertFactory = alertFactory;
        this.alertQueryService = alertQueryService;
        this.clock = clock;
    }

    /**
     * Idempotent create: one source ticket becomes exactly one alert.
     * Sending the same ticketNumber again returns the stored alert unchanged.
     *
     * Deliberately NOT @Transactional. If the insert hits the unique constraint inside an
     * outer transaction, that transaction is marked rollback-only and the re-read below
     * would fail. Here saveAndFlush commits (or fails) in its own transaction, and the
     * re-read runs in a fresh one. The children are inserted with the alert (cascade).
     */
    public CreateAlertResult create(CreateAlertRequest request) {
        Optional<Long> existingId = alertRepository.findIdByTicketNumber(request.ticketNumber());
        if (existingId.isPresent()) {
            return CreateAlertResult.alreadyExisted(alertQueryService.getById(existingId.get()));
        }

        Alert alert = alertFactory.create(request);
        // Crossing the module border: ask the playbook module, get ids back, never touch its entities.
        playbookService.findActiveFor(alert.getCaseType())
                .ifPresent(p -> alert.attachPlaybook(p.id(), p.version(), p.stepIds()));

        try {
            Alert saved = alertRepository.saveAndFlush(alert);
            return CreateAlertResult.newlyCreated(alertQueryService.getById(saved.getId()));
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same ticket both passed the check above; the other one
            // inserted first and the database constraint stopped us. Its row is the alert.
            // If no row exists, the insert failed for another reason: rethrow.
            Long winnerId = alertRepository.findIdByTicketNumber(request.ticketNumber()).orElseThrow(() -> e);
            return CreateAlertResult.alreadyExisted(alertQueryService.getById(winnerId));
        }
    }

    // Summaries only: the list never touches the child tables.
    @Transactional(readOnly = true)
    public PageResponse<AlertSummaryResponse> list(int page, int size) {
        Page<AlertSummaryResponse> alerts = alertRepository
                .findAll(Paging.pageRequest(page, size, NEWEST_FIRST))
                .map(AlertSummaryResponse::from);
        return PageResponse.from(alerts);
    }

    /**
     * Partial update. The rules live in Alert's methods (closed → 409, assigning a NEW alert → INVESTIGATING).
     * Two layers stop lost updates:
     * 1. the client's version (if sent) must match the stored one, or the change is refused straight away;
     * 2. @Version: if another request commits between our read and our write, Hibernate's
     *    "UPDATE ... WHERE version = ?" matches no row and throws (also 409, in GlobalExceptionHandler).
     */
    @Transactional
    public AlertDetailResponse update(Long id, UpdateAlertRequest request) {
        Alert alert = load(id);
        if (request.version() != null && request.version() != alert.getVersion()) {
            throw new ConflictException("Alert " + id + " was changed by someone else (version "
                    + alert.getVersion() + ", you sent " + request.version() + "). Reload and try again.");
        }
        alert.ensureOpen();

        if (request.priority() != null) {
            alert.changePriority(request.priority());
        }
        if (request.assigneeId() != null) {
            alert.assignTo(request.assigneeId());
        }
        if (request.waitingOn() != null) {
            alert.changeWaitingOn(request.waitingOn());
        }
        if (request.externalCaseNumber() != null) {
            alert.changeExternalCaseNumber(request.externalCaseNumber());
        }
        if (request.followUpOn() != null) {
            alert.changeFollowUpOn(request.followUpOn());
        }

        // Flush now so the version check runs here and the response shows the new version and updatedAt.
        alertRepository.flush();
        return alertQueryService.getById(id);
    }

    /**
     * Close with the analyst's verdict. Several rules, one transaction: either every check passes and the
     * alert is closed, or nothing changes. Alert.close checks "already closed" and "pending steps";
     * the controller already refused a missing or UNDECIDED verdict (400).
     */
    @Transactional
    public AlertDetailResponse close(Long id, CloseAlertRequest request) {
        Alert alert = alertRepository.findWithStepProgressById(id)
                .orElseThrow(() -> NotFoundException.of("Alert", id));
        alert.close(request.verdict(), clock);
        alertRepository.flush();
        return alertQueryService.getById(id);
    }

    private Alert load(Long id) {
        return alertRepository.findById(id).orElseThrow(() -> NotFoundException.of("Alert", id));
    }
}

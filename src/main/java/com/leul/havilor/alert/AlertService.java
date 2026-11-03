package com.leul.havilor.alert;

import com.leul.havilor.common.ConflictException;
import com.leul.havilor.common.NotFoundException;
import com.leul.havilor.common.PageResponse;
import com.leul.havilor.common.Paging;
import com.leul.havilor.playbook.PlaybookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
public class AlertService {

    // Newest first; id breaks ties so alerts with the same receivedAt never jump between pages.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id"));

    private final AlertRepository alertRepository;
    private final PlaybookService playbookService;
    // Read-only transaction for loading an alert with its children. A TransactionTemplate (instead of
    // @Transactional) because create() needs it too, and create() calling an annotated method on
    // "this" would skip Spring's proxy, so no transaction would start.
    private final TransactionTemplate readOnlyTx;

    public AlertService(AlertRepository alertRepository, PlaybookService playbookService,
                        PlatformTransactionManager transactionManager) {
        this.alertRepository = alertRepository;
        this.playbookService = playbookService;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
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
            return CreateAlertResult.alreadyExisted(readDetail(existingId.get()));
        }

        Alert alert = request.toAlert();
        // Crossing the module border: ask the playbook module, get ids back, never touch its entities.
        playbookService.findActiveFor(alert.getCaseType())
                .ifPresent(p -> alert.attachPlaybook(p.id(), p.version(), p.stepIds()));

        try {
            Alert saved = alertRepository.saveAndFlush(alert);
            return CreateAlertResult.newlyCreated(AlertDetailResponse.from(saved));
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same ticket both passed the check above; the other one
            // inserted first and the database constraint stopped us. Its row is the alert.
            // If no row exists, the insert failed for another reason: rethrow.
            Long winnerId = alertRepository.findIdByTicketNumber(request.ticketNumber()).orElseThrow(() -> e);
            return CreateAlertResult.alreadyExisted(readDetail(winnerId));
        }
    }

    public AlertDetailResponse getById(Long id) {
        return readDetail(id);
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
        Alert alert = loadWithChildren(id);
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
        return AlertDetailResponse.from(alert);
    }

    private AlertDetailResponse readDetail(Long id) {
        return readOnlyTx.execute(status -> AlertDetailResponse.from(loadWithChildren(id)));
    }

    // Exactly 3 queries, however many children the alert has (see AlertRepository). The second and
    // third return the same Alert instance as the first, now with one more list filled in.
    // Must run inside a transaction, so all three share one persistence context.
    private Alert loadWithChildren(Long id) {
        Alert alert = alertRepository.findWithRecipientsById(id)
                .orElseThrow(() -> NotFoundException.of("Alert", id));
        alertRepository.findWithUrlsById(id);
        alertRepository.findWithAttachmentsById(id);
        return alert;
    }
}

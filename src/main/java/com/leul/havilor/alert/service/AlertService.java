package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.request.CloseAlertRequest;
import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.api.request.UpdateAlertRequest;
import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.api.response.AlertSummaryResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.domain.enums.Source;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.common.error.BadRequestException;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.common.paging.Paging;
import com.leul.havilor.finding.service.CheckFacts;
import com.leul.havilor.finding.api.response.FindingResponse;
import com.leul.havilor.finding.service.FindingService;
import com.leul.havilor.knowledge.service.KnowledgeService;
import com.leul.havilor.playbook.service.PlaybookService;
import com.leul.havilor.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    // Newest first; id breaks ties so alerts with the same receivedAt never jump between pages.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id"));

    private final AlertRepository alertRepository;
    private final PlaybookService playbookService;
    private final AlertFactory alertFactory;
    private final AlertQueryService alertQueryService;
    private final UserService userService;
    private final FindingService findingService;
    private final AuditService auditService;
    private final KnowledgeService knowledgeService;
    private final Clock clock;

    public AlertService(AlertRepository alertRepository, PlaybookService playbookService,
                        AlertFactory alertFactory, AlertQueryService alertQueryService, UserService userService,
                        FindingService findingService, AuditService auditService, KnowledgeService knowledgeService,
                        Clock clock) {
        this.alertRepository = alertRepository;
        this.playbookService = playbookService;
        this.alertFactory = alertFactory;
        this.alertQueryService = alertQueryService;
        this.userService = userService;
        this.findingService = findingService;
        this.auditService = auditService;
        this.knowledgeService = knowledgeService;
        this.clock = clock;
    }

    /**
     * One source ticket becomes exactly one alert. Sending the same ticketNumber again returns the
     * stored alert unchanged. The children are inserted with the alert (cascade).
     * If two requests with the same new ticket arrive at once, the database's unique constraint stops
     * the second insert, and GlobalExceptionHandler answers it with 409.
     * A new alert also gets its automatic findings (finding module), in the same transaction.
     */
    @Transactional
    public CreateAlertResult create(CreateAlertRequest request) {
        String ticketNumber = ticketNumberFor(request);
        Optional<Long> existingId = alertRepository.findIdByTicketNumber(ticketNumber);
        if (existingId.isPresent()) {
            return CreateAlertResult.alreadyExisted(alertQueryService.getById(existingId.get()));
        }

        Alert alert = alertFactory.create(request, ticketNumber);
        // Crossing the module border: ask the playbook module, get ids back, never touch its entities.
        playbookService.findActiveFor(alert.getCaseType())
                .ifPresent(p -> alert.attachPlaybook(p.id(), p.version(), p.stepIds()));

        CheckFacts facts = alertFactory.checkFacts(request);
        // The critical rule: someone clicked a link that is on a bad list.
        if (facts.clickedBadListUrl()) {
            alert.changePriority(Priority.CRITICAL);
        }

        Alert saved = alertRepository.save(alert);
        findingService.runAutomaticChecks(saved.getId(), facts);
        return CreateAlertResult.newlyCreated(alertQueryService.getById(saved.getId()));
    }

    // Summaries only: the list never touches the child tables.
    @Transactional(readOnly = true)
    public PageResponse<AlertSummaryResponse> list(int page, int size) {
        Page<Alert> alerts = alertRepository.findAll(Paging.pageRequest(page, size, NEWEST_FIRST));
        List<AlertSummaryResponse> rows = alertQueryService.summaries(alerts.getContent()); // + the assignees' names
        return new PageResponse<>(rows, alerts.getNumber(), alerts.getSize(), alerts.getTotalElements(),
                alerts.getTotalPages());
    }

    /**
     * Partial update. The rules live in Alert's methods (closed → 409, assigning a NEW alert → INVESTIGATING).
     * @Version: if another request commits between our read and our write, Hibernate's
     * "UPDATE ... WHERE version = ?" matches no row and throws (409, in GlobalExceptionHandler).
     */
    // Assigning (Flow F25): an ANALYST may only assign the alert to herself (else 403); a MANAGER may assign it to
    // any active ANALYST (else 400). Both roles may change the other fields.
    @Transactional
    public AlertDetailResponse update(Long id, UpdateAlertRequest request, Long actorId, boolean actorIsManager) {
        Alert alert = load(id);
        alert.ensureOpen();
        String before = snapshot(alert);

        if (request.priority() != null) {
            alert.changePriority(request.priority());
        }
        if (request.assigneeId() != null) {
            if (!actorIsManager && !request.assigneeId().equals(actorId)) {
                throw new AccessDeniedException("Analysts can only assign alerts to themselves");
            }
            // The assignee is checked through the user module's service, never its repository.
            if (actorIsManager && !userService.isActiveAnalyst(request.assigneeId())) {
                throw new BadRequestException("assigneeId", "must be an active analyst");
            }
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

        // Flush now so the response shows the new version and updatedAt.
        alertRepository.flush();
        auditService.record(actorId, "ALERT_UPDATED", AuditService.ALERT, id, before, snapshot(alert));
        return alertQueryService.getById(id);
    }

    /**
     * Close with the analyst's verdict. Several rules, one transaction: either every check passes and the
     * alert is closed, or nothing changes. Alert.close checks the verdict (UNDECIDED → 400),
     * "already closed" and "pending steps" (409).
     */
    @Transactional
    public AlertDetailResponse close(Long id, CloseAlertRequest request, Long actorId) {
        Alert alert = alertRepository.findWithStepProgressById(id)
                .orElseThrow(() -> NotFoundException.of("Alert", id));
        String before = "status=" + alert.getStatus() + ", verdict=" + alert.getVerdict();
        alert.close(request.verdict(), clock);
        alertRepository.flush();
        auditService.record(actorId, "ALERT_CLOSED", AuditService.ALERT, id, before,
                "status=" + alert.getStatus() + ", verdict=" + alert.getVerdict());
        // A closed case becomes knowledge for the AI assistant. Never let that stop the close: log and go on.
        try {
            knowledgeService.indexClosedAlert(id, knowledgeText(alert, findingService.listForAlert(id)));
        } catch (RuntimeException e) {
            log.warn("Could not index closed alert {} for knowledge search", id, e);
        }
        return alertQueryService.getById(id);
    }

    // What a closed case teaches: what it looked like, what was found, and what the analyst decided.
    static String knowledgeText(Alert alert, List<FindingResponse> findings) {
        return "Closed alert " + alert.getId() + ": subject \"" + alert.getEmail().getSubject() + "\" from "
                + alert.getEmail().getFromAddress()
                + "\nFindings: " + (findings.isEmpty() ? "none"
                        : findings.stream().map(FindingResponse::title).collect(Collectors.joining("; ")))
                + "\nVerdict: " + alert.getVerdict()
                + (alert.getSuggestionReason() == null ? "" : "\nReason: " + alert.getSuggestionReason());
    }

    // Saves the AI assistant's suggested verdict (AlertAiService). 404 / 409 like every change.
    @Transactional
    public void recordSuggestion(Long id, Verdict suggested, String reason, Long actorId) {
        Alert alert = load(id);
        String before = "suggestedVerdict=" + alert.getSuggestedVerdict();
        alert.recordSuggestion(suggested, reason);
        auditService.record(actorId, "AI_SUGGESTION", AuditService.ALERT, id, before,
                "suggestedVerdict=" + suggested + ", reason=" + reason);
    }

    // For the escalation module: escalating needs an open alert (404 / 409 otherwise).
    @Transactional
    public void escalate(Long id) {
        load(id).escalate();
    }

    // For the escalation module: the other team gave its outcome.
    @Transactional
    public void endEscalation(Long id) {
        load(id).endEscalation();
    }

    // Every source system has its own ticket number. A MANUAL alert may come without one: then it gets
    // MAN-<today, yyyyMMdd>-<n>. Two manual alerts numbered at the same moment would collide on the unique
    // ticket number: the second gets 409 and can simply be sent again.
    private String ticketNumberFor(CreateAlertRequest request) {
        if (request.ticketNumber() != null && !request.ticketNumber().isBlank()) {
            return request.ticketNumber();
        }
        if (request.source() != Source.MANUAL) {
            throw new BadRequestException("ticketNumber", "must not be blank");
        }
        String prefix = "MAN-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        return prefix + (alertRepository.countByTicketNumberStartingWith(prefix) + 1);
    }

    // The fields PATCH can change, as one line for the audit log.
    private static String snapshot(Alert alert) {
        return "status=" + alert.getStatus() + ", priority=" + alert.getPriority()
                + ", assigneeId=" + alert.getAssigneeId() + ", waitingOn=" + alert.getWaitingOn()
                + ", externalCaseNumber=" + alert.getExternalCaseNumber() + ", followUpOn=" + alert.getFollowUpOn();
    }

    private Alert load(Long id) {
        return alertRepository.findById(id).orElseThrow(() -> NotFoundException.of("Alert", id));
    }
}

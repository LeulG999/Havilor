package com.leul.havilor.alert;

import com.leul.havilor.common.NotFoundException;
import com.leul.havilor.common.PageResponse;
import com.leul.havilor.common.Paging;
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
    // Read-only transaction for loading an alert with its children. A TransactionTemplate (instead of
    // @Transactional) because create() needs it too, and create() calling an annotated method on
    // "this" would skip Spring's proxy, so no transaction would start.
    private final TransactionTemplate readOnlyTx;

    public AlertService(AlertRepository alertRepository, PlatformTransactionManager transactionManager) {
        this.alertRepository = alertRepository;
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

        try {
            Alert saved = alertRepository.saveAndFlush(request.toAlert());
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

    // Exactly 3 queries, however many children the alert has (see AlertRepository). The second and
    // third return the same Alert instance as the first, now with one more list filled in.
    private AlertDetailResponse readDetail(Long id) {
        return readOnlyTx.execute(status -> {
            Alert alert = alertRepository.findWithRecipientsById(id)
                    .orElseThrow(() -> NotFoundException.of("Alert", id));
            alertRepository.findWithUrlsById(id);
            alertRepository.findWithAttachmentsById(id);
            return AlertDetailResponse.from(alert);
        });
    }
}

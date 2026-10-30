package com.leul.havilor.alert;

import com.leul.havilor.common.PageResponse;
import com.leul.havilor.common.Paging;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AlertService {

    // Newest first; id breaks ties so alerts with the same receivedAt never jump between pages.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id"));

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Idempotent create: one source ticket becomes exactly one alert.
     * Sending the same ticketNumber again returns the stored alert unchanged.
     *
     * Deliberately NOT @Transactional. If the insert hits the unique constraint inside an
     * outer transaction, that transaction is marked rollback-only and the re-read below
     * would fail. Here saveAndFlush commits (or fails) in its own transaction, and the
     * re-read runs in a fresh one.
     */
    public CreateAlertResult create(CreateAlertRequest request) {
        Optional<Alert> existing = alertRepository.findByTicketNumber(request.ticketNumber());
        if (existing.isPresent()) {
            return CreateAlertResult.alreadyExisted(AlertResponse.from(existing.get()));
        }

        try {
            Alert saved = alertRepository.saveAndFlush(
                    new Alert(request.ticketNumber(), request.subject(), request.receivedAt()));
            return CreateAlertResult.newlyCreated(AlertResponse.from(saved));
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same ticket both passed the check above; the other one
            // inserted first and the database constraint stopped us. Its row is the alert.
            // If no row exists, the insert failed for another reason: rethrow.
            return alertRepository.findByTicketNumber(request.ticketNumber())
                    .map(winner -> CreateAlertResult.alreadyExisted(AlertResponse.from(winner)))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AlertResponse> findById(Long id) {
        return alertRepository.findById(id).map(AlertResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> list(int page, int size) {
        Page<AlertResponse> alerts = alertRepository
                .findAll(Paging.pageRequest(page, size, NEWEST_FIRST))
                .map(AlertResponse::from);
        return PageResponse.from(alerts);
    }
}

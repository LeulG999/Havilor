package com.leul.havilor.alert;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AlertService {

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
            return CreateAlertResult.existing(AlertResponse.from(existing.get()));
        }

        try {
            Alert saved = alertRepository.saveAndFlush(
                    new Alert(request.ticketNumber(), request.subject(), request.receivedAt()));
            return CreateAlertResult.created(AlertResponse.from(saved));
        } catch (DataIntegrityViolationException e) {
            // Two requests with the same ticket both passed the check above; the other one
            // inserted first and the database constraint stopped us. Its row is the alert.
            // If no row exists, the insert failed for another reason: rethrow.
            return alertRepository.findByTicketNumber(request.ticketNumber())
                    .map(winner -> CreateAlertResult.existing(AlertResponse.from(winner)))
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AlertResponse> findById(Long id) {
        return alertRepository.findById(id).map(AlertResponse::from);
    }
}

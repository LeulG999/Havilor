package com.leul.havilor.alert;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against the real PostgreSQL from compose.yaml (schema from Flyway).
// Each test runs in a transaction that is rolled back, so nothing is left behind.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @Test
    void savesAndFindsByTicketNumber() {
        String ticket = uniqueTicket();
        Alert saved = alertRepository.saveAndFlush(newAlert(ticket));

        assertThat(saved.getId()).isNotNull();
        assertThat(alertRepository.findByTicketNumber(ticket))
                .get()
                .extracting(Alert::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void databaseRejectsDuplicateTicketNumber() {
        String ticket = uniqueTicket();
        alertRepository.saveAndFlush(newAlert(ticket));

        assertThatThrownBy(() -> alertRepository.saveAndFlush(newAlert(ticket)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Alert newAlert(String ticketNumber) {
        return new Alert(ticketNumber, "Fake invoice overdue", Instant.parse("2026-10-28T14:00:00Z"));
    }

    private static String uniqueTicket() {
        return "INC-TEST-" + UUID.randomUUID();
    }
}

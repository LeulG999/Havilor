package com.leul.havilor.alert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pure unit test: no Spring, no database. The repository is a Mockito mock.
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String TICKET = "INC0010001";
    private static final Instant RECEIVED_AT = Instant.parse("2026-10-28T14:00:00Z");

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertService alertService;

    @Test
    void createsAlertWhenTicketIsNew() {
        when(alertRepository.findByTicketNumber(TICKET)).thenReturn(Optional.empty());
        when(alertRepository.saveAndFlush(any(Alert.class))).thenAnswer(call -> withId(call.getArgument(0), 1L));

        CreateAlertResult result = alertService.create(request("Fake invoice overdue"));

        assertThat(result.created()).isTrue();
        assertThat(result.alert().id()).isEqualTo(1L);
        assertThat(result.alert().status()).isEqualTo("NEW");
        assertThat(result.alert().subject()).isEqualTo("Fake invoice overdue");
    }

    @Test
    void returnsExistingAlertWhenTicketAlreadyExists() {
        Alert existing = withId(new Alert(TICKET, "Original subject", RECEIVED_AT), 7L);
        when(alertRepository.findByTicketNumber(TICKET)).thenReturn(Optional.of(existing));

        CreateAlertResult result = alertService.create(request("Different subject on resend"));

        assertThat(result.created()).isFalse();
        assertThat(result.alert().id()).isEqualTo(7L);
        assertThat(result.alert().subject()).isEqualTo("Original subject"); // stored alert is not changed
        verify(alertRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsExistingAlertWhenAConcurrentRequestWinsTheRace() {
        Alert winner = withId(new Alert(TICKET, "Fake invoice overdue", RECEIVED_AT), 9L);
        // First check: not there yet. After our insert fails: the other request's row is there.
        when(alertRepository.findByTicketNumber(TICKET)).thenReturn(Optional.empty(), Optional.of(winner));
        when(alertRepository.saveAndFlush(any(Alert.class)))
                .thenThrow(new DataIntegrityViolationException("uk_alert_ticket_number"));

        CreateAlertResult result = alertService.create(request("Fake invoice overdue"));

        assertThat(result.created()).isFalse();
        assertThat(result.alert().id()).isEqualTo(9L);
    }

    @Test
    void rethrowsWhenInsertFailsForAnotherReason() {
        when(alertRepository.findByTicketNumber(TICKET)).thenReturn(Optional.empty());
        when(alertRepository.saveAndFlush(any(Alert.class)))
                .thenThrow(new DataIntegrityViolationException("not-null violation"));

        assertThatThrownBy(() -> alertService.create(request("Fake invoice overdue")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByIdReturnsAlertWhenPresent() {
        when(alertRepository.findById(3L))
                .thenReturn(Optional.of(withId(new Alert(TICKET, "Fake invoice overdue", RECEIVED_AT), 3L)));

        assertThat(alertService.findById(3L)).get().extracting(AlertResponse::ticketNumber).isEqualTo(TICKET);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(alertService.findById(404L)).isEmpty();
    }

    private static CreateAlertRequest request(String subject) {
        return new CreateAlertRequest(TICKET, subject, RECEIVED_AT);
    }

    // The database sets the id; in a unit test we set it ourselves.
    private static Alert withId(Alert alert, Long id) {
        ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }
}

package com.leul.havilor.alert;

import com.leul.havilor.common.CaseType;
import com.leul.havilor.common.ConflictException;
import com.leul.havilor.common.NotFoundException;
import com.leul.havilor.common.PageResponse;
import com.leul.havilor.playbook.ActivePlaybook;
import com.leul.havilor.playbook.PlaybookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pure unit test: no Spring, no database. The repository is a Mockito mock, and so is the
// transaction manager (the TransactionTemplate just runs the callback).
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String TICKET = "INC0010001";
    private static final Instant RECEIVED_AT = Instant.parse("2026-10-28T14:00:00Z");

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private PlaybookService playbookService;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, playbookService, mock(PlatformTransactionManager.class));
    }

    @Test
    void createsAlertWithItsChildrenWhenTicketIsNew() {
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.empty());
        when(alertRepository.saveAndFlush(any(Alert.class))).thenAnswer(call -> withId(call.getArgument(0), 1L));

        CreateAlertResult result = alertService.create(request("Fake invoice overdue"));

        assertThat(result.wasCreated()).isTrue();
        assertThat(result.alert().id()).isEqualTo(1L);
        assertThat(result.alert().status()).isEqualTo(AlertStatus.NEW);
        assertThat(result.alert().verdict()).isEqualTo(Verdict.UNDECIDED);
        assertThat(result.alert().email().subject()).isEqualTo("Fake invoice overdue");
        assertThat(result.alert().recipients()).extracting(RecipientResponse::address)
                .containsExactly("victim@corp.example.com");
    }

    @Test
    void newAlertFollowsTheActivePlaybookForItsCaseType() {
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.empty());
        when(playbookService.findActiveFor(CaseType.DETECTED_PHISHING))
                .thenReturn(Optional.of(new ActivePlaybook(1L, 1, List.of(11L, 12L, 13L))));
        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        when(alertRepository.saveAndFlush(saved.capture())).thenAnswer(call -> withId(call.getArgument(0), 2L));

        CreateAlertResult result = alertService.create(request("Fake"));

        assertThat(result.alert().playbookId()).isEqualTo(1L);
        assertThat(result.alert().playbookVersion()).isEqualTo(1);
        assertThat(saved.getValue().getStepProgress())
                .extracting(StepProgress::getPlaybookStepId, StepProgress::getStatus)
                .containsExactly(tuple(11L, StepStatus.PENDING),
                        tuple(12L, StepStatus.PENDING),
                        tuple(13L, StepStatus.PENDING));
    }

    @Test
    void childrenPointBackToTheirAlert() {
        Alert alert = TestAlerts.request(TICKET, "Fake", RECEIVED_AT).toAlert();

        assertThat(alert.getRecipients()).allSatisfy(r -> assertThat(r.getAlert()).isSameAs(alert));
    }

    @Test
    void returnsExistingAlertWhenTicketAlreadyExists() {
        Alert existing = withId(TestAlerts.alert(TICKET, "Original subject", RECEIVED_AT), 7L);
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.of(7L));
        stubDetailLoad(existing);

        CreateAlertResult result = alertService.create(request("Different subject on resend"));

        assertThat(result.wasCreated()).isFalse();
        assertThat(result.alert().id()).isEqualTo(7L);
        assertThat(result.alert().email().subject()).isEqualTo("Original subject"); // stored alert is not changed
        verify(alertRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsExistingAlertWhenAConcurrentRequestWinsTheRace() {
        Alert winner = withId(TestAlerts.alert(TICKET, "Fake invoice overdue", RECEIVED_AT), 9L);
        // First check: not there yet. After our insert fails: the other request's row is there.
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.empty(), Optional.of(9L));
        when(alertRepository.saveAndFlush(any(Alert.class)))
                .thenThrow(new DataIntegrityViolationException("uk_alert_ticket_number"));
        stubDetailLoad(winner);

        CreateAlertResult result = alertService.create(request("Fake invoice overdue"));

        assertThat(result.wasCreated()).isFalse();
        assertThat(result.alert().id()).isEqualTo(9L);
    }

    @Test
    void rethrowsWhenInsertFailsForAnotherReason() {
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.empty());
        when(alertRepository.saveAndFlush(any(Alert.class)))
                .thenThrow(new DataIntegrityViolationException("not-null violation"));

        assertThatThrownBy(() -> alertService.create(request("Fake invoice overdue")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getByIdReturnsAlertWhenPresent() {
        stubDetailLoad(withId(TestAlerts.alert(TICKET, "Fake invoice overdue", RECEIVED_AT), 3L));

        assertThat(alertService.getById(3L).ticketNumber()).isEqualTo(TICKET);
    }

    @Test
    void getByIdThrowsNotFoundWhenMissing() {
        when(alertRepository.findWithRecipientsById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getById(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Alert 404 not found");
    }

    @Test
    void listAsksForNewestFirstAndMapsToSummaries() {
        Alert alert = withId(TestAlerts.alert(TICKET, "Fake invoice overdue", RECEIVED_AT), 5L);
        when(alertRepository.findAll(any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(alert), call.getArgument(0), 41));

        PageResponse<AlertSummaryResponse> page = alertService.list(2, 20);

        ArgumentCaptor<Pageable> asked = ArgumentCaptor.forClass(Pageable.class);
        verify(alertRepository).findAll(asked.capture());
        assertThat(asked.getValue().getPageNumber()).isEqualTo(2);
        assertThat(asked.getValue().getPageSize()).isEqualTo(20);
        assertThat(asked.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id")));

        assertThat(page.content()).extracting(AlertSummaryResponse::id).containsExactly(5L);
        assertThat(page.content()).extracting(AlertSummaryResponse::subject).containsExactly("Fake invoice overdue");
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(41);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void listCapsOversizedPages() {
        when(alertRepository.findAll(any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(), call.getArgument(0), 0));

        PageResponse<AlertSummaryResponse> page = alertService.list(0, 500);

        assertThat(page.size()).isEqualTo(100);
    }

    @Test
    void updateChangesOnlyTheFieldsThatWereSent() {
        Alert alert = withId(TestAlerts.alert(TICKET, "Fake", RECEIVED_AT), 11L);
        stubDetailLoad(alert);

        AlertDetailResponse updated = alertService.update(11L,
                new UpdateAlertRequest(null, 7L, null, null, null, null));

        assertThat(updated.assigneeId()).isEqualTo(7L);
        assertThat(updated.status()).isEqualTo(AlertStatus.INVESTIGATING);
        assertThat(updated.priority()).isEqualTo(Priority.MEDIUM);
        verify(alertRepository).flush();
    }

    @Test
    void updateRefusesAStaleVersion() {
        Alert alert = withId(TestAlerts.alert(TICKET, "Fake", RECEIVED_AT), 12L);
        ReflectionTestUtils.setField(alert, "version", 3L);
        stubDetailLoad(alert);

        assertThatThrownBy(() -> alertService.update(12L,
                new UpdateAlertRequest(Priority.LOW, null, null, null, null, 2L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("version 3, you sent 2");
        assertThat(alert.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    private void stubDetailLoad(Alert alert) {
        when(alertRepository.findWithRecipientsById(alert.getId())).thenReturn(Optional.of(alert));
        when(alertRepository.findWithUrlsById(alert.getId())).thenReturn(Optional.of(alert));
        when(alertRepository.findWithAttachmentsById(alert.getId())).thenReturn(Optional.of(alert));
    }

    private static CreateAlertRequest request(String subject) {
        return TestAlerts.request(TICKET, subject, RECEIVED_AT);
    }

    // The database sets the id; in a unit test we set it ourselves.
    private static Alert withId(Alert alert, Long id) {
        ReflectionTestUtils.setField(alert, "id", id);
        return alert;
    }
}

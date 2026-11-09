package com.leul.havilor.alert;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.api.request.UpdateAlertRequest;
import com.leul.havilor.alert.api.response.AlertDetailResponse;
import com.leul.havilor.alert.api.response.AlertSummaryResponse;
import com.leul.havilor.alert.api.response.RecipientResponse;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.StepProgress;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.domain.enums.StepStatus;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.alert.repository.AlertRepository;
import com.leul.havilor.alert.service.AlertFactory;
import com.leul.havilor.alert.service.AlertQueryService;
import com.leul.havilor.alert.service.AlertService;
import com.leul.havilor.alert.service.CreateAlertResult;
import com.leul.havilor.common.CaseType;
import com.leul.havilor.common.error.BadRequestException;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.common.paging.PageResponse;
import com.leul.havilor.playbook.service.ActivePlaybook;
import com.leul.havilor.playbook.service.PlaybookService;
import com.leul.havilor.user.service.UserService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pure unit test: no Spring, no database. The repository is a Mockito mock; AlertQueryService is the real
// class on top of it (without Spring its @Transactional does nothing, which is fine here).
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String TICKET = "INC0010001";
    private static final Instant RECEIVED_AT = Instant.parse("2026-10-28T14:00:00Z");

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private PlaybookService playbookService;

    @Mock
    private UserService userService;

    private AlertQueryService alertQueryService;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertQueryService = new AlertQueryService(alertRepository);
        alertService = new AlertService(alertRepository, playbookService, new AlertFactory(),
                alertQueryService, userService, FixedClockConfiguration.CLOCK);
    }

    @Test
    void createsAlertWithItsChildrenWhenTicketIsNew() {
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.empty());
        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        when(alertRepository.save(saved.capture())).thenAnswer(call -> withId(call.getArgument(0), 1L));
        when(alertRepository.findById(1L)).thenAnswer(call -> Optional.of(saved.getValue()));

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
        when(alertRepository.save(saved.capture())).thenAnswer(call -> withId(call.getArgument(0), 2L));
        when(alertRepository.findById(2L)).thenAnswer(call -> Optional.of(saved.getValue()));

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
        Alert alert = new AlertFactory().create(TestAlerts.request(TICKET, "Fake", RECEIVED_AT));

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
        verify(alertRepository, never()).save(any());
    }

    @Test
    void aFailedInsertIsNotCaught() {
        // e.g. another request inserted the same ticket first; GlobalExceptionHandler makes it a 409.
        when(alertRepository.findIdByTicketNumber(TICKET)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class)))
                .thenThrow(new DataIntegrityViolationException("uk_alert_ticket_number"));

        assertThatThrownBy(() -> alertService.create(request("Fake invoice overdue")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getByIdReturnsAlertWhenPresent() {
        stubDetailLoad(withId(TestAlerts.alert(TICKET, "Fake invoice overdue", RECEIVED_AT), 3L));

        assertThat(alertQueryService.getById(3L).ticketNumber()).isEqualTo(TICKET);
    }

    @Test
    void getByIdThrowsNotFoundWhenMissing() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertQueryService.getById(404L))
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
        when(userService.exists(7L)).thenReturn(true);

        AlertDetailResponse updated = alertService.update(11L,
                new UpdateAlertRequest(null, 7L, null, null, null));

        assertThat(updated.assigneeId()).isEqualTo(7L);
        assertThat(updated.status()).isEqualTo(AlertStatus.INVESTIGATING);
        assertThat(updated.priority()).isEqualTo(Priority.MEDIUM);
        verify(alertRepository).flush();
    }

    @Test
    void updateRefusesAnAssigneeWhoIsNotAUser() {
        Alert alert = withId(TestAlerts.alert(TICKET, "Fake", RECEIVED_AT), 12L);
        when(alertRepository.findById(12L)).thenReturn(Optional.of(alert));
        when(userService.exists(99L)).thenReturn(false);

        assertThatThrownBy(() -> alertService.update(12L, new UpdateAlertRequest(null, 99L, null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("no user with id 99");
        assertThat(alert.getAssigneeId()).isNull();
    }

    private void stubDetailLoad(Alert alert) {
        when(alertRepository.findById(alert.getId())).thenReturn(Optional.of(alert));
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

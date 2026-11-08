package com.leul.havilor.alert;

import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.enums.AlertStatus;
import com.leul.havilor.alert.domain.enums.Priority;
import com.leul.havilor.alert.domain.enums.StepStatus;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.common.error.BadRequestException;
import com.leul.havilor.common.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.leul.havilor.FixedClockConfiguration.CLOCK;
import static com.leul.havilor.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The business rules live on the entity, so they are tested without Spring or a database.
class AlertTest {

    private final Alert alert = TestAlerts.alert("INC-UNIT-1", "Fake", Instant.parse("2026-10-28T14:00:00Z"));

    @Test
    void assigningANewAlertStartsTheInvestigation() {
        alert.assignTo(7L);

        assertThat(alert.getAssigneeId()).isEqualTo(7L);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.INVESTIGATING);
    }

    @Test
    void reassigningKeepsTheCurrentStatus() {
        ReflectionTestUtils.setField(alert, "status", AlertStatus.ESCALATED);

        alert.assignTo(8L);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ESCALATED);
    }

    @Test
    void blankTextClearsWaitingFields() {
        alert.changeWaitingOn("  Vendor security team ");
        alert.changeExternalCaseNumber("EXT-1");
        assertThat(alert.getWaitingOn()).isEqualTo("Vendor security team");

        alert.changeWaitingOn("");
        alert.changeExternalCaseNumber(" ");

        assertThat(alert.getWaitingOn()).isNull();
        assertThat(alert.getExternalCaseNumber()).isNull();
    }

    @Test
    void closingRecordsTheVerdictAndClearsWaiting() {
        alert.changeWaitingOn("Vendor security team");

        alert.close(Verdict.MALICIOUS, CLOCK);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.CLOSED);
        assertThat(alert.getVerdict()).isEqualTo(Verdict.MALICIOUS);
        assertThat(alert.getClosedAt()).isEqualTo(NOW);
        assertThat(alert.getWaitingOn()).isNull();
    }

    @Test
    void closingNeedsEveryStepFinished() {
        alert.attachPlaybook(1L, 1, List.of(11L, 12L, 13L));
        alert.recordStep(11L, StepStatus.DONE, "Spoofed", CLOCK);

        assertThatThrownBy(() -> alert.close(Verdict.MALICIOUS, CLOCK))
                .isInstanceOf(ConflictException.class)
                .hasMessageEndingWith("has 2 pending steps. Finish or skip every step before closing.");

        alert.recordStep(12L, StepStatus.SKIPPED, null, CLOCK);
        alert.recordStep(13L, StepStatus.DONE, "Clean", CLOCK);
        alert.close(Verdict.MALICIOUS, CLOCK);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.CLOSED);
    }

    @Test
    void aStepChangeTouchesTheAlert() {
        alert.attachPlaybook(1L, 1, List.of(11L));
        Instant later = NOW.plusSeconds(60);

        alert.recordStep(11L, StepStatus.DONE, null, Clock.fixed(later, ZoneOffset.UTC));

        // A changed alert column is what makes Hibernate raise @Version.
        assertThat(alert.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void closingTwiceOrWithoutADecisionIsRefused() {
        assertThatThrownBy(() -> alert.close(Verdict.UNDECIDED, CLOCK)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> alert.close(null, CLOCK)).isInstanceOf(BadRequestException.class);

        alert.close(Verdict.BENIGN, CLOCK);

        assertThatThrownBy(() -> alert.close(Verdict.MALICIOUS, CLOCK))
                .isInstanceOf(ConflictException.class)
                .hasMessageEndingWith("is already closed");
        assertThat(alert.getVerdict()).isEqualTo(Verdict.BENIGN);
    }

    @Test
    void aClosedAlertCannotChange() {
        ReflectionTestUtils.setField(alert, "status", AlertStatus.CLOSED);

        assertThatThrownBy(() -> alert.assignTo(7L)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> alert.changePriority(Priority.LOW)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> alert.changeFollowUpOn(LocalDate.of(2026, 11, 10)))
                .isInstanceOf(ConflictException.class);
        assertThat(alert.getAssigneeId()).isNull();
        assertThat(alert.getPriority()).isEqualTo(Priority.MEDIUM);
    }
}

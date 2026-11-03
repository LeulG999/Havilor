package com.leul.havilor.alert;

import com.leul.havilor.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

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

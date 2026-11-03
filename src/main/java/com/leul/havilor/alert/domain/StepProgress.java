package com.leul.havilor.alert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// The association class between Alert and PlaybookStep: how far this alert is with that step.
// It lives in the alert module and refers to the step by id only. It never holds a PlaybookStep
// entity, because that belongs to the playbook module (border rule).
@Entity
@Table(name = "step_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StepProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(name = "playbook_step_id", nullable = false)
    private Long playbookStepId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepStatus status;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "completed_at")
    private Instant completedAt;

    StepProgress(Long playbookStepId) {
        this.playbookStepId = playbookStepId;
        this.status = StepStatus.PENDING;
    }

    void belongTo(Alert alert) {
        this.alert = alert;
    }

    boolean isPending() {
        return status == StepStatus.PENDING;
    }

    // Finishing sets completedAt; going back to PENDING clears it (the DB CHECK enforces the same pairing).
    void record(StepStatus newStatus, String newAnswer) {
        this.status = newStatus;
        this.answer = newAnswer;
        this.completedAt = newStatus == StepStatus.PENDING ? null : Instant.now();
    }
}

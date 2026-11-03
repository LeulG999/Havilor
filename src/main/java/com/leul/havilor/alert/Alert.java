package com.leul.havilor.alert;

import com.leul.havilor.common.CaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "alert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA needs it; our code uses the constructor below
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    // EnumType.STRING stores the name ("CRITICAL"). The default, ORDINAL, stores the position (0, 1...)
    // and silently breaks old rows the day someone reorders or inserts an enum value.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 30)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertStatus status;

    // Havilor suggests (Slice 3), the analyst decides.
    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_verdict", length = 30)
    private Verdict suggestedVerdict;

    @Column(name = "suggestion_reason", columnDefinition = "TEXT")
    private String suggestionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Verdict verdict;

    // Just the id for now: the user module (and the FK) arrives in Slice 4.
    @Column(name = "assignee_id")
    private Long assigneeId;

    // "Waiting on external" as three fields (it was the ExternalCase class before class diagram v3).
    @Column(name = "waiting_on", length = 200)
    private String waitingOn;

    @Column(name = "external_case_number", length = 50)
    private String externalCaseNumber;

    @Column(name = "follow_up_on")
    private LocalDate followUpOn;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Embedded
    private EmailDetails email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // A new alert always starts as NEW and UNDECIDED; the caller can't choose either.
    public Alert(String ticketNumber, Source source, CaseType caseType, Priority priority,
                 Instant receivedAt, EmailDetails email) {
        this.ticketNumber = ticketNumber;
        this.source = source;
        this.caseType = caseType;
        this.priority = priority;
        this.receivedAt = receivedAt;
        this.email = email;
        this.status = AlertStatus.NEW;
        this.verdict = Verdict.UNDECIDED;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

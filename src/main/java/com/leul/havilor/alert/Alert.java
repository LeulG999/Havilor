package com.leul.havilor.alert;

import com.leul.havilor.common.CaseType;
import com.leul.havilor.common.ConflictException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Alert owns its children: they are saved, updated and deleted with it (cascade ALL), and a child
    // removed from the list is deleted from the table (orphanRemoval). mappedBy = the child's "alert"
    // field holds the foreign key. Collections are LAZY by default; AlertService loads them on purpose.
    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Recipient> recipients = new ArrayList<>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Url> urls = new ArrayList<>();

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Attachment> attachments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Optimistic locking: Hibernate adds "AND version = ?" to every UPDATE and increments it.
    // A stale update fails instead of silently overwriting someone else's change.
    @Version
    @Column(nullable = false)
    private long version;

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

    // ---- Behaviour. There are no setters: every change goes through a method that knows the rules. ----

    // A closed alert is final. Every change method checks this first.
    public void ensureOpen() {
        if (status == AlertStatus.CLOSED) {
            throw new ConflictException("Alert " + id + " is closed and can't be changed");
        }
    }

    public void changePriority(Priority newPriority) {
        ensureOpen();
        this.priority = newPriority;
    }

    // Rule: picking up a NEW alert means someone is now working on it.
    public void assignTo(Long userId) {
        ensureOpen();
        this.assigneeId = userId;
        if (status == AlertStatus.NEW) {
            this.status = AlertStatus.INVESTIGATING;
        }
    }

    // Blank text clears the field (null means "not sent", handled by the caller).
    public void changeWaitingOn(String waitingOn) {
        ensureOpen();
        this.waitingOn = blankToNull(waitingOn);
    }

    public void changeExternalCaseNumber(String externalCaseNumber) {
        ensureOpen();
        this.externalCaseNumber = blankToNull(externalCaseNumber);
    }

    public void changeFollowUpOn(LocalDate followUpOn) {
        ensureOpen();
        this.followUpOn = followUpOn;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    // The add methods keep both sides in sync: the list here and the child's alert field (which is what
    // JPA actually writes as alert_id). Callers get read-only views, so nobody can skip these methods.
    public void addRecipient(Recipient recipient) {
        recipients.add(recipient);
        recipient.belongTo(this);
    }

    public void addUrl(Url url) {
        urls.add(url);
        url.belongTo(this);
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        attachment.belongTo(this);
    }

    public List<Recipient> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }

    public List<Url> getUrls() {
        return Collections.unmodifiableList(urls);
    }

    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
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

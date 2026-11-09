package com.leul.havilor.escalation.domain;

import com.leul.havilor.escalation.domain.enums.EscalationOutcome;
import com.leul.havilor.escalation.domain.enums.ExternalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// An alert handed to another team. The alert is another module's: we keep its id only.
@Entity
@Table(name = "escalation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Column(nullable = false, length = 100)
    private String team;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_status", nullable = false, length = 20)
    private ExternalStatus externalStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EscalationOutcome outcome;

    public Escalation(Long alertId, String team, String reason) {
        this.alertId = alertId;
        this.team = team;
        this.reason = reason;
        this.externalStatus = ExternalStatus.OPEN;
    }

    public void changeExternalStatus(ExternalStatus externalStatus) {
        this.externalStatus = externalStatus;
    }

    public void recordOutcome(EscalationOutcome outcome) {
        this.outcome = outcome;
    }
}

package com.leul.havilor.alert.domain;

import com.leul.havilor.alert.domain.enums.ActionResult;
import com.leul.havilor.alert.domain.enums.ActionType;
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

import java.time.Instant;

// One thing the analyst did about an alert. A record of the past: never changed after it is written.
// It keeps the alert's id only; the alert doesn't hold a list of its actions (nothing needs it loaded).
@Entity
@Table(name = "action")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionResult result;

    @Column(length = 2000)
    private String note;

    // The user who did it (from the access token).
    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    public Action(Long alertId, ActionType type, ActionResult result, String note, Long performedBy,
                  Instant performedAt) {
        this.alertId = alertId;
        this.type = type;
        this.result = result;
        this.note = note;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
    }
}

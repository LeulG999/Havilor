package com.leul.havilor.finding.domain;

import com.leul.havilor.finding.domain.enums.FindingSource;
import com.leul.havilor.finding.domain.enums.FindingType;
import com.leul.havilor.finding.domain.enums.Severity;
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

// One thing found on an alert. Never changed after it is written.
// alertId is just the id: the alert lives in another module (no JPA relation across modules).
@Entity
@Table(name = "finding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FindingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 300)
    private String title;

    // "Why this matters": the teaching part, shown to the analyst next to the finding.
    @Column(name = "teaching_text", columnDefinition = "TEXT")
    private String teachingText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Finding(Long alertId, FindingType type, Severity severity, String title, String teachingText,
                   FindingSource source, Instant createdAt) {
        this.alertId = alertId;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.teachingText = teachingText;
        this.source = source;
        this.createdAt = createdAt;
    }
}

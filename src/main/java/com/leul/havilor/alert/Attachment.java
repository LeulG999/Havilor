package com.leul.havilor.alert;

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

import java.util.Locale;

// One file attached to the email. We keep its name, type and hash, never the file itself.
@Entity
@Table(name = "attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    // Lower-cased so the same file always has the same hash text (for lookups in Slice 2).
    @Column(length = 64)
    private String sha256;

    // The sandbox / analyst verdict for this file, if known.
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Verdict verdict;

    public Attachment(String fileName, String fileType, String sha256, Verdict verdict) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.sha256 = sha256 != null ? sha256.toLowerCase(Locale.ROOT) : null;
        this.verdict = verdict;
    }

    void belongTo(Alert alert) {
        this.alert = alert;
    }
}

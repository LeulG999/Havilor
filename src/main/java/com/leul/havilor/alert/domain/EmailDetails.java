package com.leul.havilor.alert.domain;

import com.leul.havilor.alert.domain.enums.AuthResult;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// The email part of an alert. @Embeddable: no table of its own, its fields are columns in the alert table.
// It has no id and no life outside its alert (composition), which is why it is not an entity.
@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // for the builder
public class EmailDetails {

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "from_address", length = 320)
    private String fromAddress;

    // The address the mail server saw (SMTP MAIL FROM). A mismatch with fromAddress is a classic phishing sign.
    @Column(name = "envelope_sender", length = 320)
    private String envelopeSender;

    @Column(name = "sender_header", length = 500)
    private String senderHeader;

    @Column(name = "reply_to", length = 320)
    private String replyTo;

    @Column(name = "x_mailer", length = 200)
    private String xMailer;

    @Enumerated(EnumType.STRING)
    @Column(name = "spf_result", length = 10)
    private AuthResult spfResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "dkim_result", length = 10)
    private AuthResult dkimResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "dmarc_result", length = 10)
    private AuthResult dmarcResult;

    @Column(name = "phish_score")
    private Integer phishScore;

    @Column(name = "spam_score")
    private Integer spamScore;

    @Column(name = "filter_action", length = 50)
    private String filterAction;

    @Column(name = "quarantine_folder", length = 100)
    private String quarantineFolder;

    @Column(name = "raw_headers", columnDefinition = "TEXT")
    private String rawHeaders;
}

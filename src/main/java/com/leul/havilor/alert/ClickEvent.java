package com.leul.havilor.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// One recipient clicking one URL of the email. Created only through Url.addClick, so it always
// connects two children of the same alert.
@Entity
@Table(name = "click_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    // true: the click went through to the site; false: the email filter blocked it.
    @Column(nullable = false)
    private boolean permitted;

    ClickEvent(Url url, Recipient recipient, Instant clickedAt, boolean permitted) {
        this.url = url;
        this.recipient = recipient;
        this.clickedAt = clickedAt;
        this.permitted = permitted;
    }
}

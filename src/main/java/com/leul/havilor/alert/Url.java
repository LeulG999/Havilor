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

import java.net.URI;
import java.util.Locale;

// One link found in the email. Created and removed only through its Alert (Alert.addUrl).
@Entity
@Table(name = "url")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(nullable = false, length = 2048)
    private String address;

    @Column(length = 253)
    private String domain;

    // Set by the reputation check in Slice 2, or by the source system if it already knows.
    @Column(name = "on_bad_list", nullable = false)
    private boolean onBadList;

    // domain may be null: then it is taken from the address (lower-cased host), if the address parses.
    public Url(String address, String domain, boolean onBadList) {
        this.address = address;
        this.domain = domain != null ? domain.toLowerCase(Locale.ROOT) : hostOf(address);
        this.onBadList = onBadList;
    }

    void belongTo(Alert alert) {
        this.alert = alert;
    }

    // Phishing links are often deliberately malformed, so a link that doesn't parse is kept, just without a domain.
    private static String hostOf(String address) {
        try {
            String host = URI.create(address.trim()).getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

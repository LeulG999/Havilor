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

// One person the email was sent to. Created and removed only through its Alert (Alert.addRecipient).
@Entity
@Table(name = "recipient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The owning side of the relationship: this column holds the foreign key.
    // LAZY: loading a recipient does not load its alert (the @ManyToOne default is EAGER).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(nullable = false, length = 320)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus delivery;

    public Recipient(String address, DeliveryStatus delivery) {
        this.address = address;
        this.delivery = delivery;
    }

    // Only Alert calls this, so both sides of the relationship always agree.
    void belongTo(Alert alert) {
        this.alert = alert;
    }
}

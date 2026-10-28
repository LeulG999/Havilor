package com.leul.havilor.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "alert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA needs it; our code uses the constructor below
public class Alert {

    // Flow 4 turns this into the AlertStatus enum.
    static final String STATUS_NEW = "NEW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    // A new alert always starts as NEW; the caller can't choose its status.
    public Alert(String ticketNumber, String subject, Instant receivedAt) {
        this.ticketNumber = ticketNumber;
        this.subject = subject;
        this.receivedAt = receivedAt;
        this.status = STATUS_NEW;
    }
}

package com.leul.havilor.alert.domain.enums;

// Where an alert is in its life. Only the server changes it (never the create request).
public enum AlertStatus {
    NEW,
    INVESTIGATING,
    ESCALATED,
    WAITING_ON_EXTERNAL,
    CLOSED
}

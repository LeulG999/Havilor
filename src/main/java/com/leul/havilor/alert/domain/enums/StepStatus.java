package com.leul.havilor.alert.domain.enums;

// Where one playbook step stands on one alert. DONE and SKIPPED both count as finished.
public enum StepStatus {
    PENDING,
    DONE,
    SKIPPED
}

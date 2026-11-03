package com.leul.havilor.common;

// What kind of case an alert is. Shared: alerts and playbooks both use it (a playbook is chosen by case type).
public enum CaseType {
    DETECTED_PHISHING,
    USER_REPORTED_EMAIL,
    RELEASE_REQUEST
}

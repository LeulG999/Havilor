package com.leul.havilor.finding.domain.enums;

// What was found. The first six come from the automatic checks; an analyst can use any of them, or OTHER.
public enum FindingType {
    SPF_FAIL,
    DKIM_FAIL,
    DMARC_FAIL,
    BAD_LIST_URL,
    MALICIOUS_ATTACHMENT,
    RECIPIENT_CLICKED,
    OTHER
}

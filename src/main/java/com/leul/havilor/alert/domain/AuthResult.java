package com.leul.havilor.alert.domain;

// Result of one email authentication check (SPF, DKIM or DMARC).
public enum AuthResult {
    PASS,
    FAIL,
    NONE
}

package com.leul.havilor.alert;

// Result of one email authentication check (SPF, DKIM or DMARC).
public enum AuthResult {
    PASS,
    FAIL,
    NONE
}

package com.leul.havilor.alert.domain;

// The analyst's decision about an alert (and Havilor's suggestion). UNDECIDED until she decides.
public enum Verdict {
    UNDECIDED,
    MALICIOUS,
    BENIGN,
    FALSE_POSITIVE
}

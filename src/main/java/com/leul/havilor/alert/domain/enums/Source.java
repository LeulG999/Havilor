package com.leul.havilor.alert.domain.enums;

// Which system sent the alert in.
public enum Source {
    SERVICENOW,
    PROOFPOINT_TAP,
    TEST_GENERATOR,
    MANUAL          // entered by an analyst (Flow 19: manual intake)
}

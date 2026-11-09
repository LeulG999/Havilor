package com.leul.havilor.finding.service;

import java.util.List;

// What the automatic checks need to know about a new alert, as plain values. The alert module fills it in,
// so the finding module never has to know the alert's classes (and the modules don't depend on each other
// in a circle: alert → finding only).
public record CheckFacts(
        boolean spfFailed,
        boolean dkimFailed,
        boolean dmarcFailed,
        List<String> badListUrls,
        List<String> maliciousAttachments,
        List<String> clickedRecipients,
        boolean clickedBadListUrl
) {
}

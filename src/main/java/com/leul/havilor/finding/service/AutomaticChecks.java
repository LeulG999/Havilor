package com.leul.havilor.finding.service;

import com.leul.havilor.finding.domain.Finding;
import com.leul.havilor.finding.domain.enums.FindingSource;
import com.leul.havilor.finding.domain.enums.FindingType;
import com.leul.havilor.finding.domain.enums.Severity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// The simple rule checks that run when an alert arrives. One rule that hits = one finding, with a teaching text
// that explains why it matters. Pure logic: no database, so it is easy to test.
final class AutomaticChecks {

    static final String SPF_WHY = "SPF lets a domain list the servers allowed to send its mail. A failure means the "
            + "sending server is not on that list: the message may come from someone pretending to be the domain.";
    static final String DKIM_WHY = "DKIM is a signature the sending domain puts on the message. A failure means the "
            + "signature is missing or doesn't match: the message may have been changed or not sent by that domain.";
    static final String DMARC_WHY = "DMARC checks that the domain in the visible From address passed SPF or DKIM. "
            + "A failure means the sender the user sees is not proven: a classic sign of spoofing.";
    static final String BAD_LIST_URL_WHY = "The link is on a list of known bad sites (phishing, malware). Anyone "
            + "who opened it may have entered credentials or downloaded something: check who clicked.";
    static final String MALICIOUS_ATTACHMENT_WHY = "The attachment was judged malicious. If a user opened it, the "
            + "device may be infected: it may need to be isolated and scanned.";
    static final String CLICKED_WHY = "A recipient clicked a link in this email. That moves the case from \"someone "
            + "received a phishing email\" to \"someone may have been compromised\": check that user's account.";

    private AutomaticChecks() {
    }

    static List<Finding> run(Long alertId, CheckFacts facts, Instant now) {
        List<Finding> findings = new ArrayList<>();
        if (facts.spfFailed()) {
            findings.add(automatic(alertId, FindingType.SPF_FAIL, Severity.MEDIUM, "SPF failed", SPF_WHY, now));
        }
        if (facts.dkimFailed()) {
            findings.add(automatic(alertId, FindingType.DKIM_FAIL, Severity.MEDIUM, "DKIM failed", DKIM_WHY, now));
        }
        if (facts.dmarcFailed()) {
            findings.add(automatic(alertId, FindingType.DMARC_FAIL, Severity.HIGH, "DMARC failed", DMARC_WHY, now));
        }
        if (!facts.badListUrls().isEmpty()) {
            findings.add(automatic(alertId, FindingType.BAD_LIST_URL, Severity.HIGH,
                    titled("URL on a bad list", facts.badListUrls()), BAD_LIST_URL_WHY, now));
        }
        if (!facts.maliciousAttachments().isEmpty()) {
            findings.add(automatic(alertId, FindingType.MALICIOUS_ATTACHMENT, Severity.HIGH,
                    titled("Malicious attachment", facts.maliciousAttachments()), MALICIOUS_ATTACHMENT_WHY, now));
        }
        if (!facts.clickedRecipients().isEmpty()) {
            String what = facts.clickedBadListUrl() ? "Recipient clicked a link on a bad list" : "Recipient clicked a link";
            findings.add(automatic(alertId, FindingType.RECIPIENT_CLICKED, Severity.HIGH,
                    titled(what, facts.clickedRecipients()), CLICKED_WHY, now));
        }
        return findings;
    }

    // "URL on a bad list: a" or "URL on a bad list: a (+2 more)", cut to fit the title column.
    private static String titled(String what, List<String> items) {
        String title = what + ": " + items.getFirst() + (items.size() > 1 ? " (+" + (items.size() - 1) + " more)" : "");
        return title.length() <= 300 ? title : title.substring(0, 297) + "...";
    }

    private static Finding automatic(Long alertId, FindingType type, Severity severity, String title, String why,
                                     Instant now) {
        return new Finding(alertId, type, severity, title, why, FindingSource.AUTOMATIC, now);
    }
}

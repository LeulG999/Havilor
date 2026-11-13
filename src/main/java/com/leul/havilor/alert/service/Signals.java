package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.response.AlertSummaryResponse.Signal;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.Attachment;
import com.leul.havilor.alert.domain.EmailDetails;
import com.leul.havilor.alert.domain.Url;
import com.leul.havilor.alert.domain.enums.AuthResult;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.alert.repository.ClickFlag;

import java.util.ArrayList;
import java.util.List;

// The "Signals" column of the queue (Flow D4): short tags for what an alert contains, from data Havilor already has.
// bad = it points to a threat (shown in red); otherwise it is just there (shown neutral).
//   URL   – it has links; bad if one is on a block list
//   FILE  – it has attachments; bad if one was judged MALICIOUS
//   CLICK – someone clicked a link; bad if a click was let through
//   SPF / DKIM / DMARC – shown when the check FAILED (bad) or returned NONE (neutral); a PASS is not a signal
final class Signals {

    private Signals() {
    }

    static List<Signal> of(Alert alert, List<ClickFlag> clicks) {
        List<Signal> signals = new ArrayList<>();
        if (!alert.getUrls().isEmpty()) {
            signals.add(new Signal("URL", alert.getUrls().stream().anyMatch(Url::isOnBadList)));
        }
        if (!alert.getAttachments().isEmpty()) {
            signals.add(new Signal("FILE", alert.getAttachments().stream()
                    .map(Attachment::getVerdict).anyMatch(verdict -> verdict == Verdict.MALICIOUS)));
        }
        if (!clicks.isEmpty()) {
            signals.add(new Signal("CLICK", clicks.stream().anyMatch(ClickFlag::permitted)));
        }
        EmailDetails email = alert.getEmail();
        addAuth(signals, "SPF", email.getSpfResult());
        addAuth(signals, "DKIM", email.getDkimResult());
        addAuth(signals, "DMARC", email.getDmarcResult());
        return signals;
    }

    private static void addAuth(List<Signal> signals, String check, AuthResult result) {
        if (result == AuthResult.FAIL || result == AuthResult.NONE) {
            signals.add(new Signal(check, result == AuthResult.FAIL));
        }
    }
}

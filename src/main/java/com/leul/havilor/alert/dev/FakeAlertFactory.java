package com.leul.havilor.alert.dev;

import com.leul.havilor.alert.api.request.AttachmentRequest;
import com.leul.havilor.alert.api.request.ClickRequest;
import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.api.request.EmailDetailsRequest;
import com.leul.havilor.alert.api.request.RecipientRequest;
import com.leul.havilor.alert.api.request.UrlRequest;
import com.leul.havilor.alert.domain.AuthResult;
import com.leul.havilor.alert.domain.DeliveryStatus;
import com.leul.havilor.alert.domain.Priority;
import com.leul.havilor.alert.domain.Source;
import com.leul.havilor.alert.domain.Verdict;
import com.leul.havilor.common.CaseType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.random.RandomGenerator;

// Builds realistic-looking but completely FAKE alerts (CLAUDE.md safety rule).
// Every domain is under example.com / example.net / example.org, reserved by RFC 2606 for exactly this:
// they can never belong to a real company or person.
class FakeAlertFactory {

    private static final String[] SUBJECTS = {
            "Your mailbox is almost full", "Invoice INV-%d is overdue", "Action required: verify your account",
            "Shared document: Q4 bonus plan", "Payment confirmation #%d", "Your password expires today",
            "Voicemail received (0:%d)", "DocuSign: please review and sign", "Unusual sign-in activity detected",
            "Updated bank details for our next payment"};
    private static final String[] SENDER_DOMAINS = {
            "invoices.example.net", "it-helpdesk.example.org", "secure-login.example.net",
            "docs-share.example.org", "payroll-portal.example.net", "partner-vendor.example.com"};
    private static final String[] SENDER_NAMES = {"billing", "it-support", "no-reply", "accounts", "hr", "security"};
    private static final String[] PEOPLE = {"alex", "sam", "jordan", "taylor", "casey", "morgan", "riley", "jamie"};
    private static final String[] URL_PATHS = {"/login", "/verify?id=%d", "/invoice/%d", "/reset-password", "/share/doc%d"};
    private static final String[][] ATTACHMENTS = {
            {"invoice_%d.pdf.exe", "application/x-msdownload"},
            {"Payment_Details_%d.docm", "application/vnd.ms-word.document.macroEnabled.12"},
            {"scan_%d.zip", "application/zip"},
            {"meeting_notes_%d.txt", "text/plain"},
            {"statement_%d.html", "text/html"}};

    private final RandomGenerator random;

    FakeAlertFactory(RandomGenerator random) {
        this.random = random;
    }

    CreateAlertRequest next() {
        int n = random.nextInt(1000, 10000);
        String senderDomain = pick(SENDER_DOMAINS);
        String from = pick(SENDER_NAMES) + "@" + senderDomain;
        // A third of the time, the envelope sender doesn't match the From address: a classic spoofing sign.
        String envelope = random.nextInt(3) == 0 ? "bounce@mailer" + n + ".example.net" : from;

        List<RecipientRequest> recipients = new ArrayList<>();
        for (int i = 0, count = random.nextInt(1, 5); i < count; i++) {
            recipients.add(new RecipientRequest(PEOPLE[(n + i) % PEOPLE.length] + i + "@corp.example.com",
                    random.nextInt(4) == 0 ? DeliveryStatus.QUARANTINED : DeliveryStatus.DELIVERED));
        }
        List<UrlRequest> urls = new ArrayList<>();
        for (int i = 0, count = random.nextInt(0, 4); i < count; i++) {
            urls.add(new UrlRequest("https://" + senderDomain + pick(URL_PATHS).formatted(n + i), null,
                    random.nextInt(3) == 0));
        }
        List<AttachmentRequest> attachments = new ArrayList<>();
        for (int i = 0, count = random.nextInt(0, 3); i < count; i++) {
            String[] file = pick(ATTACHMENTS);
            attachments.add(new AttachmentRequest(file[0].formatted(n + i), file[1], fakeSha256(),
                    random.nextBoolean() ? null : Verdict.MALICIOUS));
        }
        Instant receivedAt = Instant.now().minus(Duration.ofMinutes(random.nextInt(1, 3 * 24 * 60)));
        List<ClickRequest> clicks = new ArrayList<>();
        for (UrlRequest url : urls) {
            for (RecipientRequest recipient : recipients) {
                if (random.nextInt(4) == 0) {
                    clicks.add(new ClickRequest(recipient.address(), url.address(),
                            receivedAt.plus(Duration.ofMinutes(random.nextInt(1, 120))), random.nextBoolean()));
                }
            }
        }

        EmailDetailsRequest email = new EmailDetailsRequest(
                pick(SUBJECTS).formatted(n), from, envelope, "\"" + capitalize(from) + "\" <" + from + ">",
                random.nextInt(4) == 0 ? "collect@harvest" + n + ".example.org" : null,
                "FakeMailer " + random.nextInt(1, 9) + ".0",
                authResult(), authResult(), authResult(),
                random.nextInt(0, 101), random.nextInt(0, 101),
                random.nextBoolean() ? "quarantine" : "deliver", random.nextBoolean() ? "Phish" : null,
                "Received: from mx" + n + ".example.net\nX-Fake-Generator: havilor-dev");

        return new CreateAlertRequest("INC-GEN-" + HexFormat.of().toHexDigits(random.nextLong()).substring(0, 10),
                Source.TEST_GENERATOR, caseType(), pick(Priority.values()), receivedAt, email,
                recipients, urls, attachments, clicks);
    }

    // Mostly detected phishing, like a real queue.
    private CaseType caseType() {
        int roll = random.nextInt(10);
        return roll < 7 ? CaseType.DETECTED_PHISHING : roll < 9 ? CaseType.USER_REPORTED_EMAIL : CaseType.RELEASE_REQUEST;
    }

    private AuthResult authResult() {
        return pick(AuthResult.values());
    }

    private String fakeSha256() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private <T> T pick(T[] values) {
        return values[random.nextInt(values.length)];
    }

    private static String capitalize(String address) {
        String name = address.substring(0, address.indexOf('@'));
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}

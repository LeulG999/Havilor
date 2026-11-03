package com.leul.havilor.alert;

import com.leul.havilor.common.CaseType;

import java.time.Instant;
import java.util.List;

// Fake alerts for tests, in one place so every test builds them the same way.
// All addresses use the reserved example.com / example.net domains (fake data only).
final class TestAlerts {

    private TestAlerts() {
    }

    static Alert alert(String ticketNumber, String subject, Instant receivedAt) {
        return new Alert(ticketNumber, Source.TEST_GENERATOR, CaseType.DETECTED_PHISHING, Priority.MEDIUM,
                receivedAt, email(subject));
    }

    static EmailDetails email(String subject) {
        return EmailDetails.builder()
                .subject(subject)
                .fromAddress("billing@invoices.example.net")
                .build();
    }

    static CreateAlertRequest request(String ticketNumber, String subject, Instant receivedAt) {
        return new CreateAlertRequest(ticketNumber, Source.TEST_GENERATOR, CaseType.DETECTED_PHISHING,
                Priority.MEDIUM, receivedAt, emailRequest(subject),
                List.of(new RecipientRequest("victim@corp.example.com", DeliveryStatus.DELIVERED)), null, null);
    }

    static EmailDetailsRequest emailRequest(String subject) {
        return new EmailDetailsRequest(subject, "billing@invoices.example.net", null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    // The smallest valid POST body.
    static String json(String ticketNumber, String subject, String receivedAt) {
        return """
                {"ticketNumber": "%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                 "priority": "MEDIUM", "receivedAt": "%s",
                 "email": {"subject": "%s", "fromAddress": "billing@invoices.example.net"},
                 "recipients": [{"address": "victim@corp.example.com", "delivery": "DELIVERED"}]}
                """.formatted(ticketNumber, receivedAt, subject);
    }
}

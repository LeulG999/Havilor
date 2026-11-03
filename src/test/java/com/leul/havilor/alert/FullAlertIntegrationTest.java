package com.leul.havilor.alert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 4: every field survives POST → PostgreSQL → GET, and enums are stored as their names.
@SpringBootTest
@AutoConfigureMockMvc
class FullAlertIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void storesAndReturnsEveryField() throws Exception {
        String ticket = "INC-IT-" + UUID.randomUUID();
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "%s", "source": "PROOFPOINT_TAP", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "HIGH", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {
                           "subject": "Your mailbox is full",
                           "fromAddress": "it-support@helpdesk.example.com",
                           "envelopeSender": "bounce@mailer.example.net",
                           "senderHeader": "IT Support <it-support@helpdesk.example.com>",
                           "replyTo": "collect@harvest.example.net",
                           "xMailer": "FakeMailer 1.0",
                           "spfResult": "FAIL", "dkimResult": "NONE", "dmarcResult": "FAIL",
                           "phishScore": 92, "spamScore": 40,
                           "filterAction": "quarantine", "quarantineFolder": "Phish",
                           "rawHeaders": "Received: from mailer.example.net\\nX-Fake: yes"
                         },
                         "recipients": [{"address": "victim@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(ticket)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("PROOFPOINT_TAP"))
                .andExpect(jsonPath("$.caseType").value("USER_REPORTED_EMAIL"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.verdict").value("UNDECIDED"))
                .andExpect(jsonPath("$.suggestedVerdict").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.email.subject").value("Your mailbox is full"))
                .andExpect(jsonPath("$.email.envelopeSender").value("bounce@mailer.example.net"))
                .andExpect(jsonPath("$.email.replyTo").value("collect@harvest.example.net"))
                .andExpect(jsonPath("$.email.xMailer").value("FakeMailer 1.0"))
                .andExpect(jsonPath("$.email.spfResult").value("FAIL"))
                .andExpect(jsonPath("$.email.dkimResult").value("NONE"))
                .andExpect(jsonPath("$.email.phishScore").value(92))
                .andExpect(jsonPath("$.email.quarantineFolder").value("Phish"))
                .andExpect(jsonPath("$.email.rawHeaders").value("Received: from mailer.example.net\nX-Fake: yes"));

        // In the table, enums are their names (EnumType.STRING) and the email fields are plain alert columns.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT priority, status, case_type, spf_result, envelope_sender FROM alert WHERE id = ?", id);
        assertThat(row).containsEntry("priority", "HIGH")
                .containsEntry("status", "NEW")
                .containsEntry("case_type", "USER_REPORTED_EMAIL")
                .containsEntry("spf_result", "FAIL")
                .containsEntry("envelope_sender", "bounce@mailer.example.net");
    }

    @Test
    void statusAndVerdictInTheRequestAreIgnored() throws Exception {
        String ticket = "INC-IT-" + UUID.randomUUID();
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                         "priority": "LOW", "receivedAt": "2026-10-28T14:00:00Z",
                         "status": "CLOSED", "verdict": "BENIGN", "suggestedVerdict": "BENIGN",
                         "email": {"subject": "Fake", "fromAddress": "a@example.com"},
                         "recipients": [{"address": "victim@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(ticket)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.verdict").value("UNDECIDED"))
                .andExpect(jsonPath("$.suggestedVerdict").isEmpty());
    }

    @Test
    void databaseRejectsAnUnknownEnumValue() throws Exception {
        String ticket = "INC-IT-" + UUID.randomUUID();
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON)
                        .content(TestAlerts.json(ticket, "Fake", "2026-10-28T14:00:00Z")))
                .andExpect(status().isCreated());

        // The CHECK constraint catches a value Java could never read back, even from a manual UPDATE.
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE alert SET priority = 'URGENT' WHERE ticket_number = ?", ticket))
                .hasMessageContaining("ck_alert_priority");
    }
}

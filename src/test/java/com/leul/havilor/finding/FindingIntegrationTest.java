package com.leul.havilor.finding;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 13: the automatic checks run when an alert is created; analysts add manual findings.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class FindingIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";
    private static final String BAD_URL = "https://login.fake-bank.example.net/verify";
    private static final String OK_URL = "https://docs.example.org/handbook";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'"); // findings cascade
    }

    @Test
    void aSuspiciousAlertGetsOneFindingPerRuleAndBecomesCritical() throws Exception {
        long id = create(alert("FAIL", "PASS", "FAIL", "MALICIOUS", BAD_URL));

        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.priority").value("CRITICAL"));
        mockMvc.perform(get(ALERTS + "/{id}/findings", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type").value(contains(
                        "SPF_FAIL", "DMARC_FAIL", "BAD_LIST_URL", "MALICIOUS_ATTACHMENT", "RECIPIENT_CLICKED")))
                .andExpect(jsonPath("$[*].source").value(everyItem(is("AUTOMATIC"))))
                .andExpect(jsonPath("$[*].alertId").value(everyItem(is((int) id))))
                .andExpect(jsonPath("$[2].title").value("URL on a bad list: " + BAD_URL))
                .andExpect(jsonPath("$[4].title").value("Recipient clicked a link on a bad list: alex@corp.example.com"))
                .andExpect(jsonPath("$[4].teachingText").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").value(FixedClockConfiguration.NOW.toString()));
    }

    @Test
    void aClickOnAnOrdinaryLinkIsAFindingButNotCritical() throws Exception {
        long id = create(alert("PASS", "PASS", "PASS", "BENIGN", OK_URL));

        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.priority").value("MEDIUM"));
        mockMvc.perform(get(ALERTS + "/{id}/findings", id))
                .andExpect(jsonPath("$[*].type").value(contains("RECIPIENT_CLICKED")));
    }

    @Test
    void theAnalystCanAddAManualFinding() throws Exception {
        long id = create(alert("PASS", "PASS", "PASS", "BENIGN", OK_URL));

        mockMvc.perform(post(ALERTS + "/{id}/findings", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type": "OTHER", "severity": "LOW", "title": "  Sender domain registered 2 days ago ",
                         "teachingText": "New domains are cheap and often used once for phishing."}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.title").value("Sender domain registered 2 days ago"))
                .andExpect(jsonPath("$.severity").value("LOW"));

        mockMvc.perform(get(ALERTS + "/{id}/findings", id))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].source").value("MANUAL"));
    }

    @Test
    void mistakesGive400And409() throws Exception {
        long id = create(alert("PASS", "PASS", "PASS", "BENIGN", OK_URL));

        mockMvc.perform(post(ALERTS + "/{id}/findings", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"OTHER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(contains("severity", "title")));

        // The finding module can't look alerts up: an unknown alert has no findings, and adding one breaks the FK.
        mockMvc.perform(get(ALERTS + "/{id}/findings", Long.MAX_VALUE)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(post(ALERTS + "/{id}/findings", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"OTHER\", \"severity\": \"LOW\", \"title\": \"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data"));
    }

    private long create(String json) throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }

    // One recipient who clicked the one URL. All fake: example.com / .net / .org only.
    private static String alert(String spf, String dkim, String dmarc, String attachmentVerdict, String url) {
        boolean bad = url.equals(BAD_URL);
        return """
                {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                 "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                 "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net",
                           "spfResult": "%s", "dkimResult": "%s", "dmarcResult": "%s"},
                 "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}],
                 "urls": [{"address": "%s", "onBadList": %s}],
                 "attachments": [{"fileName": "invoice.html", "verdict": "%s"}],
                 "clicks": [{"recipientAddress": "alex@corp.example.com", "urlAddress": "%s",
                             "clickedAt": "2026-10-28T14:03:00Z", "permitted": true}]}
                """.formatted(UUID.randomUUID(), spf, dkim, dmarc, url, bad, attachmentVerdict, url);
    }
}

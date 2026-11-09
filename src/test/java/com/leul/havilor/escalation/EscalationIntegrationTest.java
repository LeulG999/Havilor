package com.leul.havilor.escalation;

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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 15: escalate an alert to another team and get the answer back.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class EscalationIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'"); // escalations cascade
    }

    @Test
    void escalateThenTheOutcomeSendsTheAlertBackToInvestigating() throws Exception {
        long alertId = createAlert();

        String body = escalate(alertId, "{\"team\": \" Email security \", \"reason\": \"Need a tenant-wide purge\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertId").value(alertId))
                .andExpect(jsonPath("$.team").value("Email security"))
                .andExpect(jsonPath("$.externalStatus").value("OPEN"))
                .andExpect(jsonPath("$.outcome").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long escalationId = Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
        alertStatusIs(alertId, "ESCALATED");

        // Progress only: the alert stays escalated.
        update(escalationId, "{\"externalStatus\": \"IN_PROGRESS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalStatus").value("IN_PROGRESS"));
        alertStatusIs(alertId, "ESCALATED");

        // The answer: back to our investigation.
        update(escalationId, "{\"externalStatus\": \"RESOLVED\", \"outcome\": \"CONFIRMED_MALICIOUS\"}")
                .andExpect(jsonPath("$.externalStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.outcome").value("CONFIRMED_MALICIOUS"));
        alertStatusIs(alertId, "INVESTIGATING");

        mockMvc.perform(get(ALERTS + "/{id}/escalations", alertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].outcome").value("CONFIRMED_MALICIOUS"));
    }

    @Test
    void aClosedAlertCannotBeEscalated() throws Exception {
        long alertId = createAlert();
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED', verdict = 'BENIGN' WHERE id = ?", alertId);

        escalate(alertId, "{\"team\": \"Legal\", \"reason\": \"x\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alert " + alertId + " is closed and can't be changed"));
        mockMvc.perform(get(ALERTS + "/{id}/escalations", alertId)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void mistakesGive400And404() throws Exception {
        long alertId = createAlert();
        escalate(alertId, "{\"team\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(contains("reason", "team")));
        alertStatusIs(alertId, "NEW");

        escalate(Long.MAX_VALUE, "{\"team\": \"Legal\", \"reason\": \"x\"}").andExpect(status().isNotFound());
        mockMvc.perform(get(ALERTS + "/{id}/escalations", Long.MAX_VALUE)).andExpect(status().isNotFound());
        update(Long.MAX_VALUE, "{\"outcome\": \"NOT_MALICIOUS\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Escalation " + Long.MAX_VALUE + " not found"));
        update(1, "{\"outcome\": \"MAYBE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("outcome"));
    }

    private void alertStatusIs(long alertId, String status) throws Exception {
        mockMvc.perform(get(ALERTS + "/{id}", alertId)).andExpect(jsonPath("$.status").value(status));
    }

    private ResultActions escalate(long alertId, String json) throws Exception {
        return mockMvc.perform(post(ALERTS + "/{id}/escalations", alertId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long escalationId, String json) throws Exception {
        return mockMvc.perform(patch("/api/v1/escalations/{id}", escalationId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createAlert() throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

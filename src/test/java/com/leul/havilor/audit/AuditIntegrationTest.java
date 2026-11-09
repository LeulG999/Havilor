package com.leul.havilor.audit;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 16: every change on an alert leaves one audit entry, written in the same transaction as the change.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class AuditIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'"); // the audit log stays
    }

    @Test
    void theAlertsWholeHistoryInOrder() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");

        send(patch(ALERTS + "/{id}", id), "{\"priority\": \"HIGH\", \"assigneeId\": %d}".formatted(testUsers.analystId()));
        send(post(ALERTS + "/{id}/actions", id), "{\"type\": \"BLOCK_SENDER\", \"result\": \"DONE\"}");
        send(post(ALERTS + "/{id}/findings", id), "{\"type\": \"OTHER\", \"severity\": \"LOW\", \"title\": \"New domain\"}");
        String escalation = send(post(ALERTS + "/{id}/escalations", id), "{\"team\": \"Legal\", \"reason\": \"Review\"}")
                .andReturn().getResponse().getContentAsString();
        long escalationId = Long.parseLong(escalation.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
        send(patch("/api/v1/escalations/{id}", escalationId), "{\"outcome\": \"NOT_MALICIOUS\"}");
        send(post(ALERTS + "/{id}/close", id), "{\"verdict\": \"BENIGN\"}");

        mockMvc.perform(get(ALERTS + "/{id}/audit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action").value(contains("ALERT_UPDATED", "ACTION_RECORDED",
                        "FINDING_ADDED", "ESCALATED", "ESCALATION_UPDATED", "ALERT_CLOSED")))
                .andExpect(jsonPath("$[*].entityType").value(everyItem(is("ALERT"))))
                .andExpect(jsonPath("$[*].entityId").value(everyItem(is((int) id))))
                .andExpect(jsonPath("$[*].actorId").value(everyItem(is(testUsers.analystId().intValue()))))
                .andExpect(jsonPath("$[0].oldValue").value("status=NEW, priority=MEDIUM, assigneeId=null, "
                        + "waitingOn=null, externalCaseNumber=null, followUpOn=null"))
                .andExpect(jsonPath("$[0].newValue").value("status=INVESTIGATING, priority=HIGH, assigneeId="
                        + testUsers.analystId() + ", waitingOn=null, externalCaseNumber=null, followUpOn=null"))
                .andExpect(jsonPath("$[4].oldValue").value("escalation " + escalationId + ": externalStatus=OPEN, outcome=null"))
                .andExpect(jsonPath("$[4].newValue").value(
                        "escalation " + escalationId + ": externalStatus=OPEN, outcome=NOT_MALICIOUS"))
                .andExpect(jsonPath("$[5].oldValue").value("status=INVESTIGATING, verdict=UNDECIDED"))
                .andExpect(jsonPath("$[5].newValue").value("status=CLOSED, verdict=BENIGN"))
                .andExpect(jsonPath("$[0].createdAt").value(FixedClockConfiguration.NOW.toString()));
    }

    @Test
    void aStepChangeIsAudited() throws Exception {
        long id = createAlert("DETECTED_PHISHING");
        Long stepId = jdbcTemplate.queryForObject(
                "SELECT playbook_step_id FROM step_progress WHERE alert_id = ? ORDER BY id LIMIT 1", Long.class, id);

        send(patch(ALERTS + "/{id}/steps/{stepId}", id, stepId), "{\"status\": \"DONE\", \"answer\": \"Spoofed\"}");

        mockMvc.perform(get(ALERTS + "/{id}/audit", id))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("STEP_UPDATED"))
                .andExpect(jsonPath("$[0].oldValue").value("step " + stepId + ": PENDING"))
                .andExpect(jsonPath("$[0].newValue").value("step " + stepId + ": DONE, answer=Spoofed"));
    }

    @Test
    void aRefusedChangeLeavesNoEntry() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED', verdict = 'BENIGN' WHERE id = ?", id);

        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"priority\": \"LOW\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get(ALERTS + "/{id}/audit", id)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void onlyAManagerReadsTheWholeLog() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");
        send(post(ALERTS + "/{id}/actions", id), "{\"type\": \"PURGE_EMAIL\", \"result\": \"DONE\"}");

        mockMvc.perform(get("/api/v1/audit").param("size", "1").header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("ACTION_RECORDED")) // newest first
                .andExpect(jsonPath("$.content[0].entityId").value(id))
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/api/v1/audit")) // the default token is the analyst's
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Your role is not allowed to do this"));
    }

    @Test
    void theDatabaseRefusesToChangeHistory() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");
        send(post(ALERTS + "/{id}/actions", id), "{\"type\": \"OTHER\", \"result\": \"DONE\"}");

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE audit_entry SET new_value = 'tampered' WHERE entity_id = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_entry WHERE entity_id = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }

    @Test
    void anAlertWithoutHistoryHasAnEmptyLog() throws Exception {
        mockMvc.perform(get(ALERTS + "/{id}/audit", Long.MAX_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String json) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is2xxSuccessful());
    }

    private long createAlert(String caseType) throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "%s",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID(), caseType)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

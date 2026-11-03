package com.leul.havilor.alert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 9: POST /api/v1/alerts/{id}/close and its rules, against PostgreSQL.
@SpringBootTest
@AutoConfigureMockMvc
class AlertCloseIntegrationTest {

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
    void closesWhenEveryStepIsFinished() throws Exception {
        long id = createAlert("DETECTED_PHISHING");
        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"waitingOn\": \"Vendor security team\"}")).andExpect(status().isOk());
        finishAllStepsThroughTheApi(id);

        close(id, "{\"verdict\": \"MALICIOUS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.verdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty())
                .andExpect(jsonPath("$.waitingOn").isEmpty())
                .andExpect(jsonPath("$.recipients.length()").value(1));

        String stored = jdbcTemplate.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, id);
        assertThat(stored).isEqualTo("CLOSED");
    }

    @Test
    void pendingStepsGive409AndNothingChanges() throws Exception {
        long id = createAlert("DETECTED_PHISHING");

        close(id, "{\"verdict\": \"BENIGN\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Alert " + id + " has 8 pending steps. Finish or skip every step before closing."));

        String stored = jdbcTemplate.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, id);
        assertThat(stored).isEqualTo("NEW");
    }

    @Test
    void anAlertWithoutAPlaybookClosesStraightAway() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");

        close(id, "{\"verdict\": \"FALSE_POSITIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FALSE_POSITIVE"));
    }

    @Test
    void closingTwiceGives409() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");
        close(id, "{\"verdict\": \"BENIGN\"}").andExpect(status().isOk());

        close(id, "{\"verdict\": \"MALICIOUS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alert " + id + " is already closed"));
    }

    @Test
    void aVerdictIsRequiredAndMustBeADecision() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");

        close(id, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("verdict"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be null"));
        close(id, "{\"verdict\": \"UNDECIDED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("verdict"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value("must be a decision (MALICIOUS, BENIGN or FALSE_POSITIVE), not UNDECIDED"));
        close(Long.MAX_VALUE, "{\"verdict\": \"BENIGN\"}").andExpect(status().isNotFound());
    }

    @Test
    void aStepChangeBumpsTheAlertVersion() throws Exception {
        // OPTIMISTIC_FORCE_INCREMENT: working a step counts as changing the alert, so a close that started
        // from the old version can't commit over it.
        long id = createAlert("DETECTED_PHISHING");
        long before = version(id);

        Long stepId = jdbcTemplate.queryForObject(
                "SELECT playbook_step_id FROM step_progress WHERE alert_id = ? ORDER BY id LIMIT 1", Long.class, id);
        mockMvc.perform(patch(ALERTS + "/{id}/steps/{stepId}", id, stepId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"DONE\"}")).andExpect(status().isOk());

        assertThat(version(id)).isGreaterThan(before);
    }

    private void finishAllStepsThroughTheApi(long id) throws Exception {
        for (Long stepId : jdbcTemplate.queryForList(
                "SELECT playbook_step_id FROM step_progress WHERE alert_id = ?", Long.class, id)) {
            mockMvc.perform(patch(ALERTS + "/{id}/steps/{stepId}", id, stepId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                    .andExpect(status().isOk());
        }
    }

    private long version(long id) {
        Long version = jdbcTemplate.queryForObject("SELECT version FROM alert WHERE id = ?", Long.class, id);
        return version == null ? -1 : version;
    }

    private ResultActions close(long id, String json) throws Exception {
        return mockMvc.perform(post(ALERTS + "/{id}/close", id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createAlert(String caseType) throws Exception {
        String json = TestAlerts.json("INC-IT-" + UUID.randomUUID(), "Fake", "2026-10-28T14:00:00Z")
                .replace("\"DETECTED_PHISHING\"", "\"" + caseType + "\"");
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

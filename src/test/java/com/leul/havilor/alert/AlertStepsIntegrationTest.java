package com.leul.havilor.alert;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

import static org.assertj.core.api.Assertions.assertThat;
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

// Flow 8: a new phishing alert follows the active playbook; its steps are read and worked through the API.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class AlertStepsIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void aNewPhishingAlertFollowsTheActivePlaybookWithEightPendingSteps() throws Exception {
        long id = createAlert("DETECTED_PHISHING");

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(jsonPath("$.playbookId").value(phishingPlaybookId()))
                .andExpect(jsonPath("$.playbookVersion").value(1));

        mockMvc.perform(get(ALERTS + "/{id}/steps", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[*].orderNo", contains(1, 2, 3, 4, 5, 6, 7, 8)))
                .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))))
                .andExpect(jsonPath("$[0].title").value("Check alert details"))
                .andExpect(jsonPath("$[0].whyItMatters").isNotEmpty())
                .andExpect(jsonPath("$[4].appliesWhen").value("Someone clicked a URL"))
                .andExpect(jsonPath("$[0].completedAt").isEmpty());
    }

    @Test
    void aCaseTypeWithoutAnActivePlaybookHasNoSteps() throws Exception {
        long id = createAlert("USER_REPORTED_EMAIL");

        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.playbookId").isEmpty());
        mockMvc.perform(get(ALERTS + "/{id}/steps", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void workingAStepRecordsItAndStartsTheInvestigation() throws Exception {
        long id = createAlert("DETECTED_PHISHING");
        long stepId = stepId(id, 1);

        patchStep(id, stepId, "{\"status\": \"DONE\", \"answer\": \"Spoofed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepId").value(stepId))
                .andExpect(jsonPath("$.title").value("Check alert details"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.answer").value("Spoofed"))
                .andExpect(jsonPath("$.completedAt").value(FixedClockConfiguration.NOW.toString()));

        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.status").value("INVESTIGATING"));
        mockMvc.perform(get(ALERTS + "/{id}/steps", id))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        // Undo: back to PENDING clears completedAt.
        patchStep(id, stepId, "{\"status\": \"PENDING\"}")
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }

    @Test
    void readingStepsTakesTwoQueries() throws Exception {
        long id = createAlert("DETECTED_PHISHING");
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get(ALERTS + "/{id}/steps", id)).andExpect(jsonPath("$", hasSize(8)));

        // Alert + its progress rows, then the playbook + its steps. Not 1 + 8.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void mistakesGive400404And409() throws Exception {
        long id = createAlert("DETECTED_PHISHING");
        long stepId = stepId(id, 2);

        patchStep(id, stepId, "{\"answer\": \"no status\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        patchStep(id, Long.MAX_VALUE, "{\"status\": \"DONE\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Step " + Long.MAX_VALUE + " is not part of alert " + id));
        mockMvc.perform(get(ALERTS + "/{id}/steps", Long.MAX_VALUE)).andExpect(status().isNotFound());

        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED' WHERE id = ?", id);
        patchStep(id, stepId, "{\"status\": \"SKIPPED\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alert " + id + " is closed and can't be changed"));
    }

    @Test
    void databaseRejectsTheSameStepTwiceOnOneAlert() throws Exception {
        long id = createAlert("DETECTED_PHISHING");
        long stepId = stepId(id, 1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO step_progress (alert_id, playbook_step_id, status) VALUES (?, ?, 'PENDING')",
                        id, stepId))
                .hasMessageContaining("uk_step_progress_alert_step");
    }

    private ResultActions patchStep(long alertId, long stepId, String json) throws Exception {
        return mockMvc.perform(patch(ALERTS + "/{id}/steps/{stepId}", alertId, stepId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long stepId(long alertId, int orderNo) {
        Long id = jdbcTemplate.queryForObject("""
                SELECT s.id FROM step_progress p JOIN playbook_step s ON s.id = p.playbook_step_id
                WHERE p.alert_id = ? AND s.order_no = ?""", Long.class, alertId, orderNo);
        assertThat(id).isNotNull();
        return id;
    }

    private long phishingPlaybookId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM playbook WHERE case_type = 'DETECTED_PHISHING' AND active", Long.class);
        assertThat(id).isNotNull();
        return id;
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

package com.leul.havilor.alert;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 14: the analyst records what she did on an alert.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class AlertActionIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'"); // actions cascade
    }

    @Test
    void recordsWhoDidWhatAndWhen() throws Exception {
        long id = createAlert();

        record(id, "{\"type\": \"BLOCK_SENDER\", \"result\": \"DONE\", \"note\": \" Blocked invoices.example.net \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertId").value(id))
                .andExpect(jsonPath("$.type").value("BLOCK_SENDER"))
                .andExpect(jsonPath("$.result").value("DONE"))
                .andExpect(jsonPath("$.note").value("Blocked invoices.example.net"))
                .andExpect(jsonPath("$.performedBy").value(testUsers.analystId()))
                .andExpect(jsonPath("$.performedAt").value(FixedClockConfiguration.NOW.toString()));
        // performedBy follows the token: the manager records the second one.
        mockMvc.perform(post(ALERTS + "/{id}/actions", id).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\": \"RESET_PASSWORD\", \"result\": \"PENDING\"}"))
                .andExpect(jsonPath("$.performedBy").value(testUsers.managerId()))
                .andExpect(jsonPath("$.note").doesNotExist());

        mockMvc.perform(get(ALERTS + "/{id}/actions", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type").value(contains("BLOCK_SENDER", "RESET_PASSWORD")));
    }

    @Test
    void aClosedAlertTakesNoNewActionsButCanStillBeRead() throws Exception {
        long id = createAlert();
        record(id, "{\"type\": \"PURGE_EMAIL\", \"result\": \"DONE\"}").andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED', verdict = 'MALICIOUS' WHERE id = ?", id);

        record(id, "{\"type\": \"NOTIFY_USER\", \"result\": \"DONE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alert " + id + " is closed and can't be changed"));
        mockMvc.perform(get(ALERTS + "/{id}/actions", id)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void mistakesGive400And404() throws Exception {
        long id = createAlert();
        record(id, "{\"type\": \"SHUT_DOWN_INTERNET\", \"result\": \"DONE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
        record(id, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(contains("result", "type")));

        record(Long.MAX_VALUE, "{\"type\": \"OTHER\", \"result\": \"DONE\"}").andExpect(status().isNotFound());
        mockMvc.perform(get(ALERTS + "/{id}/actions", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    private ResultActions record(long alertId, String json) throws Exception {
        return mockMvc.perform(post(ALERTS + "/{id}/actions", alertId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createAlert() throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON)
                        .content(TestAlerts.json("INC-IT-" + UUID.randomUUID(), "Fake invoice overdue",
                                "2026-10-28T14:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

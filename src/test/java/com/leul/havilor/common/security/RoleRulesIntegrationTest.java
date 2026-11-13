package com.leul.havilor.common.security;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow F25: what a MANAGER and an ANALYST may do, enforced by the backend. The default token is the analyst's.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class RoleRulesIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";
    private static final String FORBIDDEN = "Your role is not allowed to do this";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void theInvestigationIsAnalystOnly() throws Exception {
        long id = createAlert();
        Long stepId = jdbcTemplate.queryForObject(
                "SELECT playbook_step_id FROM step_progress WHERE alert_id = ? ORDER BY id LIMIT 1", Long.class, id);
        long escalationId = idOf(json(post(ALERTS + "/{id}/escalations", id), "{\"team\": \"IR\", \"reason\": \"Clicks\"}")
                .andExpect(status().isCreated()));

        // Each of these is refused for the manager, before anything changes.
        asManager(patch(ALERTS + "/{id}/steps/{stepId}", id, stepId), "{\"status\": \"DONE\"}");
        asManager(post(ALERTS + "/{id}/findings", id), "{\"type\": \"OTHER\", \"severity\": \"LOW\", \"title\": \"x\"}");
        asManager(post(ALERTS + "/{id}/actions", id), "{\"type\": \"OTHER\", \"result\": \"DONE\"}");
        asManager(post(ALERTS + "/{id}/ai/assessment", id), "{}");
        asManager(post(ALERTS + "/{id}/ai/draft-email", id), "{\"type\": \"USER_NOTICE\"}");
        asManager(post(ALERTS + "/{id}/escalations", id), "{\"team\": \"IR\", \"reason\": \"Again\"}");
        asManager(patch("/api/v1/escalations/{id}", escalationId), "{\"outcome\": \"NOT_MALICIOUS\"}");
        asManager(post(ALERTS + "/{id}/close", id), "{\"verdict\": \"BENIGN\"}");

        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.status").value("ESCALATED")); // unchanged
        mockMvc.perform(get(ALERTS + "/{id}/escalations", id)).andExpect(jsonPath("$[0].outcome").doesNotExist());
        mockMvc.perform(get(ALERTS + "/{id}/actions", id)).andExpect(jsonPath("$.length()").value(0));

        // The analyst may do them (one example; the other flows' tests cover the rest with the analyst's token).
        json(patch(ALERTS + "/{id}/steps/{stepId}", id, stepId), "{\"status\": \"DONE\"}").andExpect(status().isOk());
    }

    @Test
    void bothRolesReadEverythingAndChangeThePriority() throws Exception {
        long id = createAlert();
        for (String path : new String[] {ALERTS + "/" + id, ALERTS + "/" + id + "/steps", ALERTS + "/" + id + "/findings",
                ALERTS + "/" + id + "/actions", ALERTS + "/" + id + "/escalations", ALERTS + "/" + id + "/audit", ALERTS}) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                    .andExpect(status().isOk());
        }
        json(patch(ALERTS + "/{id}", id).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()),
                "{\"priority\": \"HIGH\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));
        json(patch(ALERTS + "/{id}", id), "{\"priority\": \"LOW\"}").andExpect(status().isOk());
    }

    @Test
    void anAnalystAssignsOnlyHerselfAManagerAssignsAnyActiveAnalyst() throws Exception {
        long id = createAlert();
        String toAnalyst = "{\"assigneeId\": %d}".formatted(testUsers.analystId());
        String toManager = "{\"assigneeId\": %d}".formatted(testUsers.managerId());

        // Analyst: someone else → 403, herself → fine.
        json(patch(ALERTS + "/{id}", id), toManager)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN));
        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.assigneeId").doesNotExist());
        json(patch(ALERTS + "/{id}", id), toAnalyst)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(testUsers.analystId()));

        // Manager: an active analyst → fine; a manager (himself) → 400.
        long other = createAlert();
        json(patch(ALERTS + "/{id}", other).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()), toAnalyst)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName").value("Test Analyst"));
        json(patch(ALERTS + "/{id}", other).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()), toManager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be an active analyst"));
    }

    @Test
    void onlyAManagerListsTheAnalysts() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "ANALYST")
                        .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem("Test Analyst")))
                .andExpect(jsonPath("$[*].name").value(not(hasItem("Test Manager"))))
                .andExpect(jsonPath("$[*].role").value(everyItem(is("ANALYST"))))
                .andExpect(jsonPath("$[*].active").value(everyItem(is(true))));

        mockMvc.perform(get("/api/v1/users").param("role", "ANALYST"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN));
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isOk()); // still for everyone
        mockMvc.perform(get("/api/v1/users").param("role", "BOSS")
                        .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                .andExpect(status().isBadRequest());
    }

    private void asManager(MockHttpServletRequestBuilder request, String body) throws Exception {
        json(request.header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()), body)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createAlert() throws Exception {
        return idOf(json(post(ALERTS), """
                {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                 "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                 "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                 "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                """.formatted(UUID.randomUUID())).andExpect(status().isCreated()));
    }

    private static long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

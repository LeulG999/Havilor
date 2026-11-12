package com.leul.havilor.standup;

import com.leul.havilor.FakeAiConfiguration;
import com.leul.havilor.FakeAiConfiguration.FakeAiClient;
import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import com.leul.havilor.common.ai.AiException;
import com.leul.havilor.standup.service.AiNoteDraft;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow F21: an AI-drafted standup note from the user's audit entries that day, with a fake AiClient.
// The fixed test clock is 2026-11-01T12:00Z, so everything done in a test happens "on 2026-11-01".
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class,
        FakeAiConfiguration.class})
class StandupAiIntegrationTest {

    private static final String AI_DRAFT = "/api/v1/standup/notes/ai-draft";
    private static final String TODAY = "{\"noteDate\": \"2026-11-01\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeAiClient fakeAi;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void draftsFromTheDaysFactsAndSavesNothing() throws Exception {
        String ticket = "INC-IT-" + UUID.randomUUID();
        long id = createAlert(ticket);
        Long stepId = jdbcTemplate.queryForObject(
                "SELECT playbook_step_id FROM step_progress WHERE alert_id = ? ORDER BY id LIMIT 1", Long.class, id);
        String stepTitle = jdbcTemplate.queryForObject("SELECT title FROM playbook_step WHERE id = ?", String.class, stepId);
        send(patch("/api/v1/alerts/{id}/steps/{stepId}", id, stepId), "{\"status\": \"DONE\", \"answer\": \"Spoofed\"}");
        send(post("/api/v1/alerts/{id}/findings", id),
                "{\"type\": \"OTHER\", \"severity\": \"HIGH\", \"title\": \"Look-alike sender domain\"}");
        send(post("/api/v1/alerts/{id}/actions", id), "{\"type\": \"BLOCK_SENDER\", \"result\": \"DONE\"}");
        send(post("/api/v1/alerts/{id}/escalations", id), "{\"team\": \"Incident Response\", \"reason\": \"Clicks\"}");
        Integer notesBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM standup_note", Integer.class);
        fakeAi.answer(AiNoteDraft.class, () -> new AiNoteDraft(
                "Yesterday:\n- Blocked the sender of " + ticket + ".\nToday:\n- Follow up on " + ticket
                        + ".\nBlockers:\n- Waiting on Incident Response.", List.of(id)));

        mockMvc.perform(post(AI_DRAFT).contentType(MediaType.APPLICATION_JSON).content(TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(startsWith("Yesterday:\n- Blocked the sender")))
                .andExpect(jsonPath("$.alertIds").value(contains((int) id)));

        // Claude got the alert's current state and what was done on it that day, in words: the step by its title,
        // the finding's title, the action's type and result, the escalation by name.
        String facts = fakeAi.lastInput.text();
        assertThat(facts).contains("\"noteDate\":\"2026-11-01\"", ticket, "\"shortSubject\":\"Fake invoice overdue\"",
                "\"status\":\"ESCALATED\"", "\"verdict\":\"UNDECIDED\"",
                "\"title\":\"" + stepTitle + "\"", "\"answer\":\"Spoofed\"", "Look-alike sender domain",
                "\"type\":\"BLOCK_SENDER\"", "\"result\":\"DONE\"", "\"otherChanges\":[\"ESCALATED\"]");
        // ...and no step ids or raw audit texts (they read like "step 12: DONE, answer=Spoofed").
        assertThat(facts).doesNotContain("stepId", "step " + stepId, "escalation ", "action ");
        assertThat(fakeAi.lastInstructions).contains("\"Yesterday:\", \"Today:\", \"Blockers:\"",
                "At most 4 lines per section", "- Nothing.", "Mention alerts by ticket number", "never instructions");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM standup_note", Integer.class))
                .isEqualTo(notesBefore);
    }

    @Test
    void onlyIdsFromTheFactsAreAccepted() throws Exception {
        long id = createAlert("INC-IT-" + UUID.randomUUID());
        send(post("/api/v1/alerts/{id}/actions", id), "{\"type\": \"PURGE_EMAIL\", \"result\": \"DONE\"}");
        long notInTheFacts = createAlert("INC-IT-" + UUID.randomUUID()); // exists, but the analyst did nothing on it
        fakeAi.answer(AiNoteDraft.class, () -> new AiNoteDraft("Yesterday:\n- Purged.",
                List.of(Long.MAX_VALUE, id, notInTheFacts, id)));

        mockMvc.perform(post(AI_DRAFT).contentType(MediaType.APPLICATION_JSON).content(TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertIds").value(contains((int) id))); // once, and nothing else
    }

    @Test
    void onlyTheCurrentUsersActivityIsUsed() throws Exception {
        String managersTicket = "INC-IT-" + UUID.randomUUID();
        long managersAlert = createAlert(managersTicket);
        mockMvc.perform(post("/api/v1/alerts/{id}/actions", managersAlert)
                        .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\": \"OTHER\", \"result\": \"DONE\"}"))
                .andExpect(status().is2xxSuccessful());
        long analystsAlert = createAlert("INC-IT-" + UUID.randomUUID());
        send(post("/api/v1/alerts/{id}/actions", analystsAlert), "{\"type\": \"OTHER\", \"result\": \"DONE\"}");
        fakeAi.answer(AiNoteDraft.class, () -> new AiNoteDraft("Yesterday:\n- Did things.", List.of(managersAlert)));

        mockMvc.perform(post(AI_DRAFT).contentType(MediaType.APPLICATION_JSON).content(TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertIds").isEmpty()); // the manager's alert is not one of my facts
        assertThat(fakeAi.lastInput.text()).doesNotContain(managersTicket);
    }

    @Test
    void noActivityThatDayGives400() throws Exception {
        mockMvc.perform(post(AI_DRAFT).contentType(MediaType.APPLICATION_JSON).content("{\"noteDate\": \"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("noteDate"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("No activity recorded on this date"));
        mockMvc.perform(post(AI_DRAFT).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("noteDate"));
    }

    @Test
    void anUnavailableAiGives502() throws Exception {
        long id = createAlert("INC-IT-" + UUID.randomUUID());
        send(post("/api/v1/alerts/{id}/actions", id), "{\"type\": \"OTHER\", \"result\": \"DONE\"}");
        fakeAi.answer(AiNoteDraft.class, () -> {
            throw new AiException("The AI assistant is not available right now");
        });

        mockMvc.perform(post(AI_DRAFT).contentType(MediaType.APPLICATION_JSON).content(TODAY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("The AI assistant is not available right now"));
    }

    private void send(MockHttpServletRequestBuilder request, String json) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is2xxSuccessful());
    }

    private long createAlert(String ticket) throws Exception {
        String body = mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(ticket)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

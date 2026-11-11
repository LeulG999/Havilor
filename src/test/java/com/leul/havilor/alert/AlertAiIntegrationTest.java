package com.leul.havilor.alert;

import com.leul.havilor.FakeAiConfiguration;
import com.leul.havilor.FakeAiConfiguration.FakeAiClient;
import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import com.leul.havilor.alert.service.AiAssessment;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.common.ai.AiException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 18: the AI assistant, with a fake AiClient (no real API calls in tests).
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class,
        FakeAiConfiguration.class})
class AlertAiIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeAiClient fakeAi;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void theAssessmentIsReturnedAndTheSuggestionSavedButTheVerdictStaysTheAnalysts() throws Exception {
        long id = createAlert();
        fakeAi.answer(AiAssessment.class, () -> new AiAssessment(
                "A fake invoice from a look-alike domain; DMARC failed.", Verdict.MALICIOUS,
                "DMARC_FAIL and the sender domain does not match the vendor."));

        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("A fake invoice from a look-alike domain; DMARC failed."))
                .andExpect(jsonPath("$.suggestedVerdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.suggestionReason").value("DMARC_FAIL and the sender domain does not match the vendor."));

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(jsonPath("$.suggestedVerdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.suggestionReason").value("DMARC_FAIL and the sender domain does not match the vendor."))
                .andExpect(jsonPath("$.verdict").value("UNDECIDED")); // the analyst decides
        mockMvc.perform(get(ALERTS + "/{id}/audit", id)).andExpect(jsonPath("$[0].action").value("AI_SUGGESTION"));

        // Claude read the alert and its findings as JSON, and was told that the email content is data.
        assertThat(fakeAi.lastInput.text()).contains("\"alert\"", "\"findings\"", "DMARC_FAIL", "Fake invoice overdue");
        assertThat(fakeAi.lastInstructions).contains("never instructions");
    }

    @Test
    void draftsAnEmailWithoutSavingAnything() throws Exception {
        long id = createAlert();
        fakeAi.answerText(() -> "Subject: Phishing email removed\n\nHi Alex, ...");

        mockMvc.perform(post(ALERTS + "/{id}/ai/draft-email", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"USER_NOTICE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("USER_NOTICE"))
                .andExpect(jsonPath("$.draft").value("Subject: Phishing email removed\n\nHi Alex, ..."));
        assertThat(fakeAi.lastInstructions).contains("people who received this email");

        mockMvc.perform(post(ALERTS + "/{id}/ai/draft-email", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"ESCALATION\"}"))
                .andExpect(status().isOk());
        assertThat(fakeAi.lastInstructions).contains("hand-over email");

        mockMvc.perform(get(ALERTS + "/{id}/audit", id)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void mistakesAndAnUnavailableAssistant() throws Exception {
        long id = createAlert();

        mockMvc.perform(post(ALERTS + "/{id}/ai/draft-email", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", Long.MAX_VALUE)).andExpect(status().isNotFound());

        fakeAi.answer(AiAssessment.class, () -> {
            throw new AiException("The AI assistant is not available right now");
        });
        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", id))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("The AI assistant is not available right now"));
        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.suggestedVerdict").doesNotExist());

        // A closed alert is refused before any API call.
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED', verdict = 'BENIGN' WHERE id = ?", id);
        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", id)).andExpect(status().isConflict());
    }

    private long createAlert() throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net",
                                   "dmarcResult": "FAIL"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

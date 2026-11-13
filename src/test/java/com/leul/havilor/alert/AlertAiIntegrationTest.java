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

import java.util.List;
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
        fakeAi.answer(AiAssessment.class, () -> new AiAssessment(Verdict.MALICIOUS, AiAssessment.Level.HIGH,
                "A fake invoice from a look-alike domain that failed DMARC.",
                List.of(new AiAssessment.Evidence("The email failed DMARC.", AiAssessment.Level.HIGH),
                        new AiAssessment.Evidence("The sender's domain is not the vendor's.", AiAssessment.Level.MEDIUM)),
                List.of("Block the sender's domain", "Remove the email from all mailboxes"),
                "The vendor confirms they sent it from this domain."));

        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedVerdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.oneLineSummary").value("A fake invoice from a look-alike domain that failed DMARC."))
                .andExpect(jsonPath("$.keyEvidence[0].text").value("The email failed DMARC."))
                .andExpect(jsonPath("$.keyEvidence[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.nextSteps[1]").value("Remove the email from all mailboxes"))
                .andExpect(jsonPath("$.wouldChangeMyMindIf").value("The vendor confirms they sent it from this domain."))
                .andExpect(jsonPath("$.references").isArray());

        // Saved: the verdict suggestion, the one-line summary as the reason, and the whole assessment as an object.
        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(jsonPath("$.suggestedVerdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.suggestionReason").value("A fake invoice from a look-alike domain that failed DMARC."))
                .andExpect(jsonPath("$.lastAssessment.confidence").value("HIGH"))
                .andExpect(jsonPath("$.lastAssessment.keyEvidence[1].severity").value("MEDIUM"))
                .andExpect(jsonPath("$.lastAssessment.nextSteps[0]").value("Block the sender's domain"))
                .andExpect(jsonPath("$.verdict").value("UNDECIDED")); // the analyst decides
        mockMvc.perform(get(ALERTS + "/{id}/audit", id)).andExpect(jsonPath("$[0].action").value("AI_SUGGESTION"));

        // Claude read the alert and its findings as JSON, was told that the email content is data, and got the
        // readability rules.
        assertThat(fakeAi.lastInput.text()).contains("\"alert\"", "\"findings\"", "DMARC_FAIL", "Fake invoice overdue");
        assertThat(fakeAi.lastInstructions).contains("never instructions", "plain everyday English", "No paragraphs",
                "Never use internal field names (such as onBadList, phishScore", "at most 25 words", "at most 6 items",
                "at most 5 short lines");
    }

    @Test
    void tooLongListsAreCut() throws Exception {
        long id = createAlert();
        List<AiAssessment.Evidence> sevenFacts = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(i -> new AiAssessment.Evidence("Fact " + i, AiAssessment.Level.LOW)).toList();
        fakeAi.answer(AiAssessment.class, () -> new AiAssessment(Verdict.UNDECIDED, AiAssessment.Level.LOW, "Unclear.",
                sevenFacts, List.of("Step 1", "Step 2", "Step 3", "Step 4", "Step 5", "Step 6"), "More facts."));

        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyEvidence.length()").value(6))
                .andExpect(jsonPath("$.nextSteps.length()").value(5));
        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.lastAssessment.keyEvidence.length()").value(6));
    }

    @Test
    void anAlertNeverAssessedHasNoLastAssessment() throws Exception {
        mockMvc.perform(get(ALERTS + "/{id}", createAlert())).andExpect(jsonPath("$.lastAssessment").doesNotExist());
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

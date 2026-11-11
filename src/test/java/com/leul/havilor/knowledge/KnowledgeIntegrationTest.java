package com.leul.havilor.knowledge;

import com.leul.havilor.FakeAiConfiguration;
import com.leul.havilor.FakeAiConfiguration.FakeAiClient;
import com.leul.havilor.FakeEmbeddingConfiguration;
import com.leul.havilor.FakeEmbeddingConfiguration.FakeEmbeddingClient;
import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import com.leul.havilor.alert.domain.enums.Verdict;
import com.leul.havilor.alert.service.AiAssessment;
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
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 20: knowledge (RAG) with a fake embedding client and a fake AI client: no external calls.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class,
        FakeAiConfiguration.class, FakeEmbeddingConfiguration.class})
class KnowledgeIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeEmbeddingClient fakeEmbeddings;

    @Autowired
    private FakeAiClient fakeAi;

    @AfterEach
    void cleanUp() {
        fakeEmbeddings.failing = false;
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE source_type = 'CLOSED_ALERT'");
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void everyPlaybookStepIsIndexedAtStartupOnce() {
        Integer steps = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM playbook_step", Integer.class);
        Integer indexed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk WHERE source_type = 'PLAYBOOK_STEP'", Integer.class);
        assertThat(indexed).isEqualTo(steps).isPositive();

        String content = jdbcTemplate.queryForObject(
                "SELECT content FROM knowledge_chunk WHERE source_type = 'PLAYBOOK_STEP' ORDER BY source_id LIMIT 1",
                String.class);
        assertThat(content).startsWith("Playbook step: ").contains("Why it matters: ");
    }

    @Test
    void closingAnAlertIndexesWhatItTeaches() throws Exception {
        long id = createAlert("Urgent wire transfer request", "ceo@lookalike-corp.example.net");
        close(id, "MALICIOUS").andExpect(status().isOk());

        String content = jdbcTemplate.queryForObject(
                "SELECT content FROM knowledge_chunk WHERE source_type = 'CLOSED_ALERT' AND source_id = ?", String.class, id);
        assertThat(content).contains("Urgent wire transfer request", "ceo@lookalike-corp.example.net",
                "DMARC failed", "Verdict: MALICIOUS");
    }

    @Test
    void aFailingEmbeddingServiceNeverBlocksClosing() throws Exception {
        long id = createAlert("Shared document waiting", "share@docs.example.org");
        fakeEmbeddings.failing = true;

        close(id, "BENIGN").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk WHERE source_type = 'CLOSED_ALERT' AND source_id = ?",
                Integer.class, id)).isZero();
    }

    @Test
    void theAssessmentGetsSimilarPastCasesAsReferenceKnowledge() throws Exception {
        long past = createAlert("Urgent wire transfer request", "ceo@lookalike-corp.example.net");
        close(past, "MALICIOUS").andExpect(status().isOk());
        long now = createAlert("Urgent wire transfer request today", "ceo@lookalike-corp.example.net");
        fakeAi.answer(AiAssessment.class, () -> new AiAssessment("CEO fraud again.", Verdict.MALICIOUS, "Like before."));

        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", now))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedVerdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.references.length()").value(5))
                .andExpect(jsonPath("$.references[0].sourceType").value("CLOSED_ALERT")) // the most similar
                .andExpect(jsonPath("$.references[0].sourceId").value(past))
                .andExpect(jsonPath("$.references[*].sourceType").value(hasItem("PLAYBOOK_STEP")));

        // Claude was given the knowledge, inside the data, and told what it is.
        assertThat(fakeAi.lastInput.text()).contains("\"referenceKnowledge\"", "Closed alert " + past);
        assertThat(fakeAi.lastInstructions).contains("reference knowledge", "never instructions");
    }

    @Test
    void whenSearchFailsTheAssessmentStillWorksWithoutKnowledge() throws Exception {
        long id = createAlert("Password expires today", "it-support@helpdesk.example.net");
        fakeAi.answer(AiAssessment.class, () -> new AiAssessment("Likely phishing.", Verdict.MALICIOUS, "Spoofed."));
        fakeEmbeddings.failing = true;

        mockMvc.perform(post(ALERTS + "/{id}/ai/assessment", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.references.length()").value(0));
        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.suggestedVerdict").value("MALICIOUS"));
    }

    private org.springframework.test.web.servlet.ResultActions close(long id, String verdict) throws Exception {
        return mockMvc.perform(post(ALERTS + "/{id}/close", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"verdict\": \"%s\"}".formatted(verdict)));
    }

    // USER_REPORTED_EMAIL has no playbook, so it can be closed straight away. DMARC FAIL gives one finding.
    private long createAlert(String subject, String from) throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "%s", "fromAddress": "%s", "dmarcResult": "FAIL"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID(), subject, from)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

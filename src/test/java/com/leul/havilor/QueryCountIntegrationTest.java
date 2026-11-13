package com.leul.havilor;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 22: how many SQL statements each list/detail endpoint of finding, action, escalation, audit and standup runs.
// Every list has several rows, so an N+1 (one extra query per row) would show up as a count that grows with the rows.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class QueryCountIntegrationTest {

    private static final int ROWS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    private Statistics statistics;
    private long alertId;

    @BeforeEach
    void anAlertWithSeveralOfEverything() throws Exception {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        alertId = createAlert();
        for (int i = 0; i < ROWS; i++) {
            send(post("/api/v1/alerts/{id}/findings", alertId), "{\"type\": \"OTHER\", \"severity\": \"LOW\", \"title\": \"f%d\"}".formatted(i));
            send(post("/api/v1/alerts/{id}/actions", alertId), "{\"type\": \"OTHER\", \"result\": \"DONE\"}");
            // One escalation at a time (Flow F31): record each team's answer before the next one.
            String escalation = mockMvc.perform(post("/api/v1/alerts/{id}/escalations", alertId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"team\": \"Legal\", \"reason\": \"r%d\"}".formatted(i)))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            send(patch("/api/v1/escalations/{id}", Long.parseLong(escalation.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"))),
                    "{\"outcome\": \"NOT_MALICIOUS\"}");
            send(post("/api/v1/standup/notes"), "{\"noteDate\": \"2026-11-0%d\", \"text\": \"QC-NOTE %d\", \"alertIds\": [%d]}"
                    .formatted(i + 1, i, alertId));
        }
        jdbcTemplate.update("UPDATE standup_note SET status = 'SHARED' WHERE text LIKE 'QC-NOTE%'");
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM standup_note WHERE text LIKE 'QC-NOTE%'");
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-QC-%'");
    }

    @Test
    void findingsOfAnAlert() throws Exception {
        assertThat(statements(get("/api/v1/alerts/{id}/findings", alertId))).isEqualTo(1);
    }

    @Test
    void actionsOfAnAlert() throws Exception {
        assertThat(statements(get("/api/v1/alerts/{id}/actions", alertId))).isEqualTo(2); // alert exists? + list
    }

    @Test
    void escalationsOfAnAlert() throws Exception {
        assertThat(statements(get("/api/v1/alerts/{id}/escalations", alertId))).isEqualTo(2); // exists + list
    }

    @Test
    void auditOfAnAlertAndTheWholeLog() throws Exception {
        assertThat(statements(get("/api/v1/alerts/{id}/audit", alertId))).isEqualTo(1);
        assertThat(statements(get("/api/v1/audit").param("size", "" + ROWS)
                .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))).isEqualTo(2); // page + count
    }

    @Test
    void ownAndTeamStandupNotes() throws Exception {
        // page + count + the mentioned alerts of all notes on the page in one batch
        assertThat(statements(get("/api/v1/standup/notes").param("size", "" + ROWS))).isEqualTo(3);
        assertThat(statements(get("/api/v1/standup/team").param("size", "" + ROWS)
                .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))).isEqualTo(3);
    }

    // Flow D4: a page of the queue with names and signals. ROWS alerts, each with a URL, a file and a click, all
    // assigned: page + count + names + clicks + URLs (@BatchSize) + attachments (@BatchSize) = 6, whatever the rows.
    @Test
    void aPageOfTheQueueWithSignals() throws Exception {
        for (int i = 0; i < ROWS; i++) {
            long id = idOf(mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"ticketNumber": "INC-QC-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                             "priority": "MEDIUM", "receivedAt": "2099-03-0%dT00:00:00Z",
                             "email": {"subject": "Fake", "fromAddress": "billing@invoices.example.net"},
                             "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}],
                             "urls": [{"address": "https://pay.example.org/x"}],
                             "attachments": [{"fileName": "notes.txt"}],
                             "clicks": [{"recipientAddress": "alex@corp.example.com", "urlAddress": "https://pay.example.org/x",
                                         "clickedAt": "2099-03-01T00:02:00Z", "permitted": false}]}
                            """.formatted(UUID.randomUUID(), i + 1))));
            send(patch("/api/v1/alerts/{id}", id), "{\"assigneeId\": %d}".formatted(testUsers.analystId()));
        }

        assertThat(statements(get("/api/v1/alerts").param("size", "" + ROWS))).isEqualTo(6);
    }

    private static long idOf(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }

    private long statements(MockHttpServletRequestBuilder request) throws Exception {
        statistics.clear();
        mockMvc.perform(request).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private void send(MockHttpServletRequestBuilder request, String json) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is2xxSuccessful());
    }

    private long createAlert() throws Exception {
        String body = mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-QC-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

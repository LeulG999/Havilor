package com.leul.havilor.dashboard;

import com.jayway.jsonpath.JsonPath;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow F26: the manager's team dashboard. Other tests share the database, so the counts are compared before/after.
// The fixed clock is 2026-11-01T12:00Z, so "today" is 2026-11-01.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class TeamDashboardIntegrationTest {

    private static final String TEAM_DASHBOARD = "/api/v1/alerts/team-dashboard";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM standup_note WHERE text LIKE 'IT-TEAM%'");
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void theNumbersMoveExactlyWithTheTeamsWork() throws Exception {
        String before = teamDashboard();

        create("MEDIUM", "2026-10-31T12:00:00Z");                        // open, unassigned
        long critical = create("CRITICAL", "2026-10-31T13:00:00Z");      // open, critical, the analyst takes it
        assign(critical);
        long closed = create("LOW", "2026-10-30T09:00:00Z");             // the analyst closed it today
        assign(closed);
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED', verdict = 'BENIGN', closed_at = ? WHERE id = ?",
                java.sql.Timestamp.from(FixedClockConfiguration.NOW), closed);
        String oldestTicket = "INC-IT-OLDEST-" + UUID.randomUUID();
        createWithTicket(oldestTicket, "LOW", "2000-01-01T00:00:00Z");  // open, unassigned, older than anything
        shareNote("IT-TEAM shared", true);
        shareNote("IT-TEAM draft", false);                               // a draft doesn't count

        String after = teamDashboard();
        assertThat(number(after, "$.unassignedOpen") - number(before, "$.unassignedOpen")).isEqualTo(2);
        assertThat(number(after, "$.criticalOpen") - number(before, "$.criticalOpen")).isEqualTo(1);
        assertThat(number(after, "$.standupsSharedToday") - number(before, "$.standupsSharedToday")).isEqualTo(1);
        assertThat(analyst(after, "openAlerts") - analyst(before, "openAlerts")).isEqualTo(1);
        assertThat(analyst(after, "closedLast7Days") - analyst(before, "closedLast7Days")).isEqualTo(1);

        assertThat((String) JsonPath.read(after, "$.oldestOpen.ticketNumber")).isEqualTo(oldestTicket);
        long expectedHours = Duration.between(Instant.parse("2000-01-01T00:00:00Z"), FixedClockConfiguration.NOW).toHours();
        assertThat(number(after, "$.oldestOpen.ageHours")).isEqualTo(expectedHours);
        // Every active analyst is listed with a name; managers are not.
        List<String> names = JsonPath.read(after, "$.analysts[*].name");
        assertThat(names).contains("Test Analyst").doesNotContain("Test Manager");
    }

    @Test
    void onlyManagersSeeIt() throws Exception {
        mockMvc.perform(get(TEAM_DASHBOARD)) // the default token is the analyst's
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your role is not allowed to do this"));
    }

    private String teamDashboard() throws Exception {
        return mockMvc.perform(get(TEAM_DASHBOARD).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long analyst(String json, String field) {
        List<Number> values = JsonPath.read(json, "$.analysts[?(@.analystId == %d)].%s"
                .formatted(testUsers.analystId(), field));
        return values.getFirst().longValue();
    }

    private static long number(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    private void assign(long alertId) throws Exception {
        mockMvc.perform(patch("/api/v1/alerts/{id}", alertId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": %d}".formatted(testUsers.analystId())))
                .andExpect(status().isOk());
    }

    private void shareNote(String text, boolean share) throws Exception {
        String body = mockMvc.perform(post("/api/v1/standup/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteDate\": \"2026-11-01\", \"text\": \"%s\"}".formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        if (share) {
            long id = ((Number) JsonPath.read(body, "$.id")).longValue();
            mockMvc.perform(post("/api/v1/standup/notes/{id}/share", id)).andExpect(status().isOk());
        }
    }

    private long create(String priority, String receivedAt) throws Exception {
        return createWithTicket("INC-IT-" + UUID.randomUUID(), priority, receivedAt);
    }

    private long createWithTicket(String ticket, String priority, String receivedAt) throws Exception {
        String body = mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "%s", "receivedAt": "%s",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(ticket, priority, receivedAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}

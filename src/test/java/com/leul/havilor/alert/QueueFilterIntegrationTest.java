package com.leul.havilor.alert;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow D4: the queue's filters (priority, status, assigned to me) and counts. Other tests share the database, so the
// test alerts arrive "in 2099" (newest first = on the first page) and counts are compared before/after.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class QueueFilterIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

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
    void filtersByPriorityStatusAndAssignee() throws Exception {
        long critical = create("CRITICAL", "2099-01-03T00:00:00Z");
        long low = create("LOW", "2099-01-02T00:00:00Z");
        long closed = create("CRITICAL", "2099-01-01T00:00:00Z");
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED', verdict = 'BENIGN' WHERE id = ?", closed);
        mockMvc.perform(patch(ALERTS + "/{id}", critical).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": %d}".formatted(testUsers.analystId())))
                .andExpect(status().isOk());

        String byPriority = list("priority=CRITICAL");
        assertThat(ids(byPriority)).contains(critical, closed).doesNotContain(low);
        assertThat(values(byPriority, "priority")).containsOnly("CRITICAL");

        String open = list("status=OPEN");
        assertThat(ids(open)).contains(critical, low).doesNotContain(closed);
        assertThat(values(open, "status")).doesNotContain("CLOSED");
        assertThat(ids(list("status=CLOSED"))).contains(closed).doesNotContain(critical, low);
        assertThat(ids(list("status=INVESTIGATING&priority=CRITICAL"))).contains(critical).doesNotContain(closed, low);

        String mine = list("assignedToMe=true");
        assertThat(ids(mine)).contains(critical).doesNotContain(low, closed);
        assertThat(values(mine, "assigneeName")).containsOnly("Test Analyst");
        String managers = mockMvc.perform(get(ALERTS + "?assignedToMe=true&size=100")
                        .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                .andReturn().getResponse().getContentAsString();
        assertThat(ids(managers)).doesNotContain(critical);
    }

    @Test
    void anUnknownStatusGives400() throws Exception {
        mockMvc.perform(get(ALERTS).param("status", "SLEEPING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        mockMvc.perform(get(ALERTS).param("priority", "URGENT")).andExpect(status().isBadRequest());
    }

    @Test
    void countsMoveWithTheQueue() throws Exception {
        String before = counts("status=OPEN");
        String mineBefore = counts("status=OPEN&assignedToMe=true");

        create("CRITICAL", "2099-01-03T00:00:00Z");                    // open, critical, unassigned
        long high = create("HIGH", "2099-01-02T00:00:00Z");           // the analyst takes it
        mockMvc.perform(patch(ALERTS + "/{id}", high).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": %d}".formatted(testUsers.analystId())))
                .andExpect(status().isOk());

        String after = counts("status=OPEN");
        assertThat(number(after, "$.open") - number(before, "$.open")).isEqualTo(2);
        assertThat(number(after, "$.criticalOpen") - number(before, "$.criticalOpen")).isEqualTo(1);
        assertThat(number(after, "$.unassignedOpen") - number(before, "$.unassignedOpen")).isEqualTo(1);
        assertThat(number(after, "$.byPriority.CRITICAL") - number(before, "$.byPriority.CRITICAL")).isEqualTo(1);
        assertThat(number(after, "$.byPriority.HIGH") - number(before, "$.byPriority.HIGH")).isEqualTo(1);

        // "Assigned to me": only the HIGH one is mine.
        String mineAfter = counts("status=OPEN&assignedToMe=true");
        assertThat(number(mineAfter, "$.byPriority.HIGH") - number(mineBefore, "$.byPriority.HIGH")).isEqualTo(1);
        assertThat(number(mineAfter, "$.byPriority.CRITICAL")).isEqualTo(number(mineBefore, "$.byPriority.CRITICAL"));
    }

    private String list(String query) throws Exception {
        return mockMvc.perform(get(ALERTS + "?size=100&" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String counts(String query) throws Exception {
        return mockMvc.perform(get(ALERTS + "/counts?" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<Long> ids(String page) {
        List<Number> ids = JsonPath.read(page, "$.content[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private static List<String> values(String page, String field) {
        return JsonPath.read(page, "$.content[*]." + field);
    }

    private static long number(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    private long create(String priority, String receivedAt) throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "%s", "receivedAt": "%s",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID(), priority, receivedAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}

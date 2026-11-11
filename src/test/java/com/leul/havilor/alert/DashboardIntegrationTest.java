package com.leul.havilor.alert;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow F13: GET /api/v1/alerts/dashboard. The test database is shared with other test classes, so the test compares
// the dashboard before and after creating known alerts (the differences), instead of absolute numbers.
// The test clock is 2026-11-01T12:00Z, so the chart covers 2026-10-19 … 2026-11-01.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class DashboardIntegrationTest {

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
    void countsChangeExactlyWithTheAlertsThatArrive() throws Exception {
        String before = dashboard(testUsers.analystBearer());
        String managerBefore = dashboard(testUsers.managerBearer());

        String newest = "INC-IT-" + UUID.randomUUID();
        create(newest, "CRITICAL", "2026-11-01T11:30:00Z");                              // open, critical
        long assigned = create("INC-IT-" + UUID.randomUUID(), "MEDIUM", "2026-10-31T08:00:00Z");
        long closed = create("INC-IT-" + UUID.randomUUID(), "LOW", "2026-10-25T23:59:59Z");
        create("INC-IT-" + UUID.randomUUID(), "LOW", "2026-10-10T09:00:00Z");           // before the chart window

        send(patch(ALERTS + "/{id}", assigned), "{\"assigneeId\": %d}".formatted(testUsers.analystId()));
        send(post(ALERTS + "/{id}/close", closed), "{\"verdict\": \"BENIGN\"}");          // closedAt = the test clock

        String after = dashboard(testUsers.analystBearer());

        assertThat(number(after, "openAlerts") - number(before, "openAlerts")).isEqualTo(3);
        assertThat(number(after, "assignedToMe") - number(before, "assignedToMe")).isEqualTo(1);
        assertThat(number(after, "criticalOpen") - number(before, "criticalOpen")).isEqualTo(1);
        assertThat(number(after, "closedLast7Days") - number(before, "closedLast7Days")).isEqualTo(1);

        // 14 UTC days, oldest first, every day present (zero days too); only the three days we used changed.
        List<Map<String, Object>> daysBefore = JsonPath.read(before, "$.alertsPerDay");
        List<Map<String, Object>> daysAfter = JsonPath.read(after, "$.alertsPerDay");
        assertThat(daysAfter).hasSize(14);
        assertThat(daysAfter.getFirst().get("date")).isEqualTo("2026-10-19");
        assertThat(daysAfter.getLast().get("date")).isEqualTo("2026-11-01");
        for (int i = 0; i < 14; i++) {
            String date = (String) daysAfter.get(i).get("date");
            long added = ((Number) daysAfter.get(i).get("count")).longValue()
                    - ((Number) daysBefore.get(i).get("count")).longValue();
            long expected = List.of("2026-10-25", "2026-10-31", "2026-11-01").contains(date) ? 1 : 0;
            assertThat(added).as(date).isEqualTo(expected);
        }

        // The newest alert is first in "recent", which holds at most 5.
        List<String> recent = JsonPath.read(after, "$.recent[*].ticketNumber");
        assertThat(recent).hasSizeLessThanOrEqualTo(5).first().isEqualTo(newest);

        // "Assigned to me" is per user: the manager's number didn't move.
        String managerAfter = dashboard(testUsers.managerBearer());
        assertThat(number(managerAfter, "assignedToMe")).isEqualTo(number(managerBefore, "assignedToMe"));
    }

    @Test
    void theDashboardNeedsALogin() throws Exception {
        mockMvc.perform(get(ALERTS + "/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    private String dashboard(String bearer) throws Exception {
        return mockMvc.perform(get(ALERTS + "/dashboard").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static long number(String json, String field) {
        return ((Number) JsonPath.read(json, "$." + field)).longValue();
    }

    private void send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String json)
            throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk());
    }

    // USER_REPORTED_EMAIL has no playbook, so it can be closed straight away.
    private long create(String ticket, String priority, String receivedAt) throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "%s", "receivedAt": "%s",
                         "email": {"subject": "Fake", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(ticket, priority, receivedAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}

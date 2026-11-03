package com.leul.havilor.alert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full app against the PostgreSQL from compose.yaml. The database may already hold other alerts,
// so test alerts use receivedAt dates in the year 2999: they are always the newest and come first.
// Test rows use the INC-IT- prefix and are deleted after each test.
@SpringBootTest
@AutoConfigureMockMvc
class AlertListIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteTestAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void defaultsToFirstPageOfTwenty() throws Exception {
        long total = alertCount();

        mockMvc.perform(get(ALERTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value((total + 19) / 20))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listsNewestFirstAcrossPages() throws Exception {
        String oldest = createAlert("2999-01-01T09:00:00Z");
        String middle = createAlert("2999-01-02T09:00:00Z");
        String newest = createAlert("2999-01-03T09:00:00Z");

        mockMvc.perform(get(ALERTS).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].ticketNumber").value(newest))
                .andExpect(jsonPath("$.content[1].ticketNumber").value(middle))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));

        mockMvc.perform(get(ALERTS).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber").value(oldest))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void sameReceivedAtIsOrderedByNewestIdFirst() throws Exception {
        String first = createAlert("2999-06-01T09:00:00Z");
        String second = createAlert("2999-06-01T09:00:00Z");

        mockMvc.perform(get(ALERTS).param("size", "2"))
                .andExpect(jsonPath("$.content[0].ticketNumber").value(second))
                .andExpect(jsonPath("$.content[1].ticketNumber").value(first));
    }

    @Test
    void capsSizeAtOneHundred() throws Exception {
        mockMvc.perform(get(ALERTS).param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void correctsOutOfRangeValues() throws Exception {
        mockMvc.perform(get(ALERTS).param("page", "-1").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    private String createAlert(String receivedAt) throws Exception {
        String ticket = "INC-IT-" + UUID.randomUUID();
        mockMvc.perform(post(ALERTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketNumber": "%s", "subject": "Fake invoice overdue", "receivedAt": "%s"}
                                """.formatted(ticket, receivedAt)))
                .andExpect(status().isCreated());
        return ticket;
    }

    private long alertCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alert", Long.class);
        return count == null ? 0 : count;
    }
}

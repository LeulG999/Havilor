package com.leul.havilor.alert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full app (controller → service → repository → PostgreSQL from compose.yaml), called over MockMvc.
// Not @Transactional: the race test needs real commits. Test rows use the INC-IT- prefix and are
// deleted after each test.
@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerIntegrationTest {

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
    void postCreatesAlertAndGetReadsItBack() throws Exception {
        String ticket = uniqueTicket();

        MvcResult created = mockMvc.perform(post(ALERTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ticket, "Fake invoice overdue")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ticketNumber").value(ticket))
                .andExpect(jsonPath("$.subject").value("Fake invoice overdue"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.receivedAt").value("2026-10-28T14:00:00Z"))
                .andReturn();
        long id = idOf(created);
        assertThat(created.getResponse().getHeader("Location")).endsWith(ALERTS + "/" + id);

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ticketNumber").value(ticket))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mockMvc.perform(get(ALERTS + "/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendingTheSameTicketTwiceReturnsTheExistingAlert() throws Exception {
        String ticket = uniqueTicket();

        long firstId = idOf(mockMvc.perform(post(ALERTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ticket, "Fake invoice overdue")))
                .andExpect(status().isCreated())
                .andReturn());

        // Same ticket, different subject: 200 with the ORIGINAL alert, nothing changed.
        mockMvc.perform(post(ALERTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ticket, "Changed subject on resend")))
                .andExpect(status().isOk())
                .andExpect(header().string("Location", endsWith(ALERTS + "/" + firstId)))
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.subject").value("Fake invoice overdue"));

        assertThat(rowsFor(ticket)).isEqualTo(1);
    }

    @Test
    void concurrentDuplicatesCreateExactlyOneAlert() throws Exception {
        String ticket = uniqueTicket();
        int requests = 8;
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests);

        try {
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                Callable<MvcResult> call = () -> {
                    startTogether.await();
                    return mockMvc.perform(post(ALERTS)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(ticket, "Fake invoice overdue")))
                            .andReturn();
                };
                futures.add(pool.submit(call));
            }
            startTogether.countDown();

            List<Integer> statuses = new ArrayList<>();
            List<Long> ids = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get();
                statuses.add(result.getResponse().getStatus());
                ids.add(idOf(result));
            }

            assertThat(statuses).containsOnly(201, 200);
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
            assertThat(ids).containsOnly(ids.getFirst());
            assertThat(rowsFor(ticket)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private int rowsFor(String ticket) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alert WHERE ticket_number = ?", Integer.class, ticket);
        return count == null ? 0 : count;
    }

    private static long idOf(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        String digits = body.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1");
        return Long.parseLong(digits);
    }

    private static String json(String ticket, String subject) {
        return """
                {"ticketNumber": "%s", "subject": "%s", "receivedAt": "2026-10-28T14:00:00Z"}
                """.formatted(ticket, subject);
    }

    private static String uniqueTicket() {
        return "INC-IT-" + UUID.randomUUID();
    }
}

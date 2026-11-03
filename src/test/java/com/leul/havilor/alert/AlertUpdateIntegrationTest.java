package com.leul.havilor.alert;

import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 6: PATCH /api/v1/alerts/{id} against PostgreSQL, including two updates racing each other.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AlertUpdateIntegrationTest {

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
    void onlySentFieldsChangeAndAssigningStartsTheInvestigation() throws Exception {
        long id = createAlert();

        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assigneeId": 7, "priority": "CRITICAL", "followUpOn": "2026-11-10"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(7))
                .andExpect(jsonPath("$.status").value("INVESTIGATING"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.followUpOn").value("2026-11-10"))
                .andExpect(jsonPath("$.email.subject").value("Fake invoice overdue")) // untouched
                .andExpect(jsonPath("$.recipients.length()").value(1))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"waitingOn": "Vendor security team", "externalCaseNumber": "EXT-2026-0042"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitingOn").value("Vendor security team"))
                .andExpect(jsonPath("$.priority").value("CRITICAL")) // not sent, so unchanged
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(jsonPath("$.externalCaseNumber").value("EXT-2026-0042"))
                .andExpect(jsonPath("$.assigneeId").value(7));
    }

    @Test
    void aClosedAlertGives409() throws Exception {
        long id = createAlert();
        // Closing arrives in Flow 9; until then, close it directly in the database.
        jdbcTemplate.update("UPDATE alert SET status = 'CLOSED' WHERE id = ?", id);

        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"priority": "LOW"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alert " + id + " is closed and can't be changed"));
    }

    @Test
    void aStaleVersionGives409() throws Exception {
        long id = createAlert();
        patchWithVersion(id, "HIGH", 0).andExpect(status().isOk());

        // The client still thinks the alert is at version 0.
        patchWithVersion(id, "LOW", 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Alert " + id + " was changed by someone else (version 1, you sent 0). Reload and try again."));

        mockMvc.perform(get(ALERTS + "/{id}", id)).andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void twoConcurrentUpdatesFromTheSameVersionLetExactlyOneWin() throws Exception {
        long id = createAlert();
        int requests = 2;
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (String priority : List.of("HIGH", "LOW")) {
                Callable<MvcResult> call = () -> {
                    startTogether.await();
                    return patchWithVersion(id, priority, 0).andReturn();
                };
                futures.add(pool.submit(call));
            }
            startTogether.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                statuses.add(future.get().getResponse().getStatus());
            }

            // Whoever is second loses: either at the version check (it read version 1) or at the
            // UPDATE ... WHERE version = 0 (it read version 0 before the winner committed).
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
            Long version = jdbcTemplate.queryForObject("SELECT version FROM alert WHERE id = ?", Long.class, id);
            assertThat(version).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void invalidAndUnknownGive400And404() throws Exception {
        mockMvc.perform(patch(ALERTS + "/{id}", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\": \"LOW\"}"))
                .andExpect(status().isNotFound());

        long id = createAlert();
        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assigneeId"));
    }

    private org.springframework.test.web.servlet.ResultActions patchWithVersion(long id, String priority, long version)
            throws Exception {
        return mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\": \"%s\", \"version\": %d}".formatted(priority, version)));
    }

    private long createAlert() throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON)
                        .content(TestAlerts.json("INC-IT-" + UUID.randomUUID(), "Fake invoice overdue",
                                "2026-10-28T14:00:00Z")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

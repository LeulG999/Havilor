package com.leul.havilor.alert;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
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
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class AlertUpdateIntegrationTest {

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
    void onlySentFieldsChangeAndAssigningStartsTheInvestigation() throws Exception {
        long id = createAlert();
        long analyst = testUsers.analystId();

        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assigneeId": %d, "priority": "CRITICAL", "followUpOn": "2026-11-10"}
                        """.formatted(analyst)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(analyst))
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
                .andExpect(jsonPath("$.assigneeId").value(analyst));
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
    void concurrentUpdatesNeverSilentlyOverwriteEachOther() throws Exception {
        long id = createAlert();
        int requests = 2;
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (String priority : List.of("HIGH", "LOW")) {
                Callable<MvcResult> call = () -> {
                    startTogether.await();
                    return patchPriority(id, priority).andReturn();
                };
                futures.add(pool.submit(call));
            }
            startTogether.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                statuses.add(future.get().getResponse().getStatus());
            }

            // If both read version 0, the second "UPDATE ... WHERE version = 0" matches no row: 409.
            // If one ran after the other committed, both succeed. Either way, every 200 is one version step.
            assertThat(statuses).isSubsetOf(200, 409).contains(200);
            Long version = jdbcTemplate.queryForObject("SELECT version FROM alert WHERE id = ?", Long.class, id);
            assertThat(version).isEqualTo(statuses.stream().filter(s -> s == 200).count());
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
        // A positive id, but nobody has it: checked through UserService.
        mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": %d}".formatted(Long.MAX_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assigneeId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("no user with id " + Long.MAX_VALUE));
    }

    private org.springframework.test.web.servlet.ResultActions patchPriority(long id, String priority)
            throws Exception {
        return mockMvc.perform(patch(ALERTS + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\": \"%s\"}".formatted(priority)));
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

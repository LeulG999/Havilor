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

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow F24: the assignee's name comes with the alert (detail, queue rows, dashboard), looked up through UserService.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class AssigneeNameIntegrationTest {

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
    void anAssignedAlertCarriesTheAssigneesName() throws Exception {
        long id = createAlert();
        mockMvc.perform(get("/api/v1/alerts/{id}", id))
                .andExpect(jsonPath("$.assigneeId").doesNotExist())
                .andExpect(jsonPath("$.assigneeName").doesNotExist()); // unassigned: null

        mockMvc.perform(patch("/api/v1/alerts/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\": %d}".formatted(testUsers.analystId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName").value("Test Analyst"));

        mockMvc.perform(get("/api/v1/alerts/{id}", id)).andExpect(jsonPath("$.assigneeName").value("Test Analyst"));
        mockMvc.perform(get("/api/v1/alerts").param("size", "100"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].assigneeName".formatted(id)).value(hasItem("Test Analyst")));
        mockMvc.perform(get("/api/v1/alerts/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recent[0].assigneeName").exists());
    }

    private long createAlert() throws Exception {
        // receivedAt far in the future: this alert is the newest, so it is recent[0] on the dashboard.
        String body = mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "MEDIUM", "receivedAt": "2099-01-01T00:00:00Z",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }
}

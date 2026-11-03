package com.leul.havilor.alert.dev;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevAlertControllerIntegrationTest {

    private static final String GENERATE = "/api/v1/dev/generate-alerts";

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    @Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
    class WithTheDevProfile {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @AfterEach
        void deleteGeneratedAlerts() {
            jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-GEN-%'");
        }

        @Test
        void generatesFakeAlertsThroughTheNormalCreatePath() throws Exception {
            mockMvc.perform(post(GENERATE).param("count", "5"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].ticketNumber").value(startsWith("INC-GEN-")));

            Integer alerts = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM alert WHERE ticket_number LIKE 'INC-GEN-%' AND source = 'TEST_GENERATOR'",
                    Integer.class);
            Integer withoutRecipients = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM alert a WHERE a.ticket_number LIKE 'INC-GEN-%'
                    AND NOT EXISTS (SELECT 1 FROM recipient r WHERE r.alert_id = a.id)""", Integer.class);
            // Phishing ones follow the playbook, like any alert created through AlertService.
            Integer phishingWithoutSteps = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM alert a WHERE a.ticket_number LIKE 'INC-GEN-%'
                    AND a.case_type = 'DETECTED_PHISHING'
                    AND NOT EXISTS (SELECT 1 FROM step_progress p WHERE p.alert_id = a.id)""", Integer.class);
            assertThat(alerts).isEqualTo(5);
            assertThat(withoutRecipients).isZero();
            assertThat(phishingWithoutSteps).isZero();
        }

        @Test
        void countIsLimited() throws Exception {
            mockMvc.perform(post(GENERATE).param("count", "500"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("count"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
    class WithoutTheDevProfile {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void theEndpointDoesNotExist() throws Exception {
            mockMvc.perform(post(GENERATE).param("count", "1"))
                    .andExpect(status().isNotFound());
        }
    }
}

package com.leul.havilor.playbook;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 7: the seeded phishing playbook, read through the API.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class PlaybookIntegrationTest {

    private static final String PLAYBOOKS = "/api/v1/playbooks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void listsTheSeededPhishingPlaybook() throws Exception {
        mockMvc.perform(get(PLAYBOOKS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Phishing investigation"))
                .andExpect(jsonPath("$[0].caseType").value("DETECTED_PHISHING"))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].steps").doesNotExist());
    }

    @Test
    void detailHasEightTeachingStepsInOrderInOneQuery() throws Exception {
        long id = phishingPlaybookId();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get(PLAYBOOKS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps", hasSize(8)))
                .andExpect(jsonPath("$.steps[*].orderNo", contains(1, 2, 3, 4, 5, 6, 7, 8)))
                .andExpect(jsonPath("$.steps[0].title").value("Check alert details"))
                .andExpect(jsonPath("$.steps[3].title").value("Check click activity"))
                .andExpect(jsonPath("$.steps[7].title").value("Communicate with the affected user and third parties"))
                .andExpect(jsonPath("$.steps[0].appliesWhen").isEmpty())
                .andExpect(jsonPath("$.steps[4].appliesWhen").value("Someone clicked a URL"))
                .andExpect(jsonPath("$.steps[1].answerOptions", contains("Malicious", "Suspicious", "Clean", "No URLs")))
                .andExpect(jsonPath("$.steps[*].whyItMatters", hasSize(8)))
                .andExpect(jsonPath("$.steps[2].whyItMatters", not(emptyOrNullString())))
                .andExpect(jsonPath("$.steps[2].toolGuide", not(emptyOrNullString())));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void unknownPlaybookGives404() throws Exception {
        mockMvc.perform(get(PLAYBOOKS + "/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Playbook " + Long.MAX_VALUE + " not found"));
    }

    @Test
    void databaseAllowsOnlyOneActivePlaybookPerCaseType() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO playbook (name, case_type, version, active) VALUES ('Copy', 'DETECTED_PHISHING', 99, TRUE)"))
                .hasMessageContaining("uk_playbook_one_active_per_case_type");
    }

    @Test
    void databaseRejectsTwoStepsWithTheSameNumber() {
        long id = phishingPlaybookId();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO playbook_step (playbook_id, order_no, title, why_it_matters, question) "
                        + "VALUES (?, 1, 'Duplicate', 'x', 'x')", id))
                .hasMessageContaining("uk_playbook_step_order");
    }

    private long phishingPlaybookId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM playbook WHERE case_type = 'DETECTED_PHISHING' AND version = 1", Long.class);
        assertThat(id).isNotNull();
        return id;
    }
}

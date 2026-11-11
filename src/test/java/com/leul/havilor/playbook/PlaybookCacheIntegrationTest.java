package com.leul.havilor.playbook;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 21: playbook reads come from Redis after the first time. Same settings as PlaybookIntegrationTest
// (SQL statistics on), so both share one Spring context.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class PlaybookCacheIntegrationTest {

    private static final String PLAYBOOKS = "/api/v1/playbooks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    private Statistics statistics;

    // Redis is shared by every test context in the run, so clear by name (getCacheNames() only lists caches this
    // context has touched so far).
    @BeforeEach
    void coldCache() {
        for (String name : new String[]{"playbooks", "playbook", "activePlaybook", "playbookSteps"}) {
            cacheManager.getCache(name).clear();
        }
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void theSecondReadIsServedFromRedisWithoutSql() throws Exception {
        long id = phishingPlaybookId();

        statistics.clear();
        mockMvc.perform(get(PLAYBOOKS + "/{id}", id)).andExpect(status().isOk());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);

        statistics.clear();
        mockMvc.perform(get(PLAYBOOKS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps.length()").value(8));
        assertThat(statistics.getPrepareStatementCount()).isZero();

        statistics.clear();
        mockMvc.perform(get(PLAYBOOKS)).andExpect(status().isOk());
        mockMvc.perform(get(PLAYBOOKS)).andExpect(jsonPath("$[0].name").value("Phishing investigation"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void entriesLiveInRedisWithTheConfiguredTimeToLive() throws Exception {
        long id = phishingPlaybookId();
        mockMvc.perform(get(PLAYBOOKS + "/{id}", id)).andExpect(status().isOk());

        assertThat(redis.hasKey("playbook::" + id)).isTrue();
        Long secondsLeft = redis.getExpire("playbook::" + id);
        assertThat(secondsLeft).isPositive().isLessThanOrEqualTo(600L); // 10m in application.yml
    }

    @Test
    void creatingAnAlertCachesItsActivePlaybookLookup() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                             "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                             "email": {"subject": "Fake", "fromAddress": "billing@invoices.example.net"},
                             "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                            """.formatted(UUID.randomUUID())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.playbookId").value(phishingPlaybookId()));
        }
        assertThat(redis.hasKey("activePlaybook::DETECTED_PHISHING")).isTrue();
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void anUnknownPlaybookIsNotCached() throws Exception {
        mockMvc.perform(get(PLAYBOOKS + "/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());

        assertThat(redis.hasKey("playbook::" + Long.MAX_VALUE)).isFalse();
    }

    private long phishingPlaybookId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM playbook WHERE case_type = 'DETECTED_PHISHING' AND active", Long.class);
        return id == null ? -1 : id;
    }
}

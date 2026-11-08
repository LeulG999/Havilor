package com.leul.havilor.alert;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 5: recipients, URLs and attachments arrive with the alert and come back on the detail view.
// Hibernate statistics are switched on here (only in this test's context) to count SQL statements.
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class AlertChildrenIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void statistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @AfterEach
    void deleteTestAlerts() {
        // ON DELETE CASCADE removes the children too.
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void childrenArriveWithTheAlertAndComeBackOnTheDetailView() throws Exception {
        String ticket = "INC-IT-" + UUID.randomUUID();
        long id = create("""
                {"ticketNumber": "%s", "source": "PROOFPOINT_TAP", "caseType": "DETECTED_PHISHING",
                 "priority": "HIGH", "receivedAt": "2026-10-28T14:00:00Z",
                 "email": {"subject": "Invoice overdue", "fromAddress": "billing@invoices.example.net"},
                 "recipients": [
                   {"address": "alex@corp.example.com", "delivery": "DELIVERED"},
                   {"address": "sam@corp.example.com", "delivery": "QUARANTINED"}],
                 "urls": [
                   {"address": "https://Login.Fake-Bank.example.net/verify?u=1", "onBadList": true},
                   {"address": "http://pay.example.org/x", "domain": "PAY.example.org"},
                   {"address": "not a url at all"}],
                 "attachments": [
                   {"fileName": "invoice.pdf.exe", "fileType": "application/x-msdownload",
                    "sha256": "%s", "verdict": "MALICIOUS"},
                   {"fileName": "notes.txt"}]}
                """.formatted(ticket, "AB".repeat(32)));

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients", hasSize(2)))
                .andExpect(jsonPath("$.recipients[0].address").value("alex@corp.example.com"))
                .andExpect(jsonPath("$.recipients[1].delivery").value("QUARANTINED"))
                .andExpect(jsonPath("$.urls", hasSize(3)))
                .andExpect(jsonPath("$.urls[0].domain").value("login.fake-bank.example.net")) // taken from the address
                .andExpect(jsonPath("$.urls[0].onBadList").value(true))
                .andExpect(jsonPath("$.urls[1].domain").value("pay.example.org"))             // given, lower-cased
                .andExpect(jsonPath("$.urls[2].domain").isEmpty())                            // unparseable: kept, no domain
                .andExpect(jsonPath("$.urls[2].onBadList").value(false))
                .andExpect(jsonPath("$.attachments", hasSize(2)))
                .andExpect(jsonPath("$.attachments[0].sha256").value("ab".repeat(32)))
                .andExpect(jsonPath("$.attachments[0].verdict").value("MALICIOUS"))
                .andExpect(jsonPath("$.attachments[1].verdict").isEmpty());

        assertThat(rows("recipient", id)).isEqualTo(2);
        assertThat(rows("url", id)).isEqualTo(3);
        assertThat(rows("attachment", id)).isEqualTo(2);
    }

    @Test
    void clicksConnectARecipientToAUrlOfTheSameAlert() throws Exception {
        long id = create("""
                {"ticketNumber": "INC-IT-%s", "source": "PROOFPOINT_TAP", "caseType": "DETECTED_PHISHING",
                 "priority": "HIGH", "receivedAt": "2026-10-28T14:00:00Z",
                 "email": {"subject": "Invoice overdue", "fromAddress": "billing@invoices.example.net"},
                 "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"},
                                {"address": "sam@corp.example.com", "delivery": "DELIVERED"}],
                 "urls": [{"address": "https://login.fake-bank.example.net/verify"}],
                 "clicks": [
                   {"recipientAddress": "SAM@corp.example.com", "urlAddress": "https://login.fake-bank.example.net/verify",
                    "clickedAt": "2026-10-28T14:07:00Z", "permitted": false},
                   {"recipientAddress": "alex@corp.example.com", "urlAddress": "https://login.fake-bank.example.net/verify",
                    "clickedAt": "2026-10-28T14:03:00Z", "permitted": true}]}
                """.formatted(UUID.randomUUID()));

        mockMvc.perform(get(ALERTS + "/{id}", id))
                .andExpect(jsonPath("$.clicks", hasSize(2)))
                .andExpect(jsonPath("$.clicks[0].recipientAddress").value("alex@corp.example.com")) // earliest first
                .andExpect(jsonPath("$.clicks[0].permitted").value(true))
                .andExpect(jsonPath("$.clicks[1].recipientAddress").value("sam@corp.example.com"))
                .andExpect(jsonPath("$.clicks[1].permitted").value(false))
                .andExpect(jsonPath("$.clicks[1].urlAddress").value("https://login.fake-bank.example.net/verify"));
    }

    @Test
    void aClickMustNameARecipientAndUrlFromTheSameRequest() throws Exception {
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                         "priority": "LOW", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake", "fromAddress": "a@example.com"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}],
                         "urls": [{"address": "https://x.example.net"}],
                         "clicks": [{"recipientAddress": "nobody@corp.example.com", "urlAddress": "https://x.example.net",
                                     "clickedAt": "2026-10-28T14:03:00Z", "permitted": true}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("clicks[0].recipientAddress"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("is not one of the recipients"));
    }

    @Test
    void listReturnsSummariesWithoutChildren() throws Exception {
        create(TestAlerts.json("INC-IT-" + UUID.randomUUID(), "Fake summary", "2999-03-01T00:00:00Z"));

        mockMvc.perform(get(ALERTS).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].subject").value("Fake summary"))
                .andExpect(jsonPath("$.content[0].fromAddress").value("billing@invoices.example.net"))
                .andExpect(jsonPath("$.content[0].recipients").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
    }

    @Test
    void detailLoadDoesNotGrowWithTheNumberOfChildren() throws Exception {
        long small = create(alertWithChildren(1));
        long big = create(alertWithChildren(25));

        // The alert, its three lazy child lists (one query each), then all clicks.
        assertThat(statementsFor(() -> mockMvc.perform(get(ALERTS + "/{id}", small)).andExpect(status().isOk())))
                .isEqualTo(5);
        assertThat(statementsFor(() -> mockMvc.perform(get(ALERTS + "/{id}", big))
                .andExpect(jsonPath("$.recipients", hasSize(25)))
                .andExpect(jsonPath("$.urls", hasSize(25)))
                .andExpect(jsonPath("$.attachments", hasSize(25)))
                .andExpect(jsonPath("$.clicks", hasSize(25)))))
                .isEqualTo(5);
    }

    @Test
    void listPageRunsTwoQueries() throws Exception {
        for (int i = 0; i < 3; i++) {
            create(alertWithChildren(5));
        }

        // One SELECT for the page, plus one COUNT for the totals. Spring Data skips the COUNT when the
        // first page already holds every row, so it is 1 or 2, never more. No child table is touched.
        assertThat(statementsFor(() -> mockMvc.perform(get(ALERTS).param("size", "20"))
                .andExpect(status().isOk())))
                .isBetween(1L, 2L);
    }

    @Test
    void anAlertNeedsAtLeastOneRecipientAndValidChildren() throws Exception {
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-X", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                         "priority": "LOW", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake", "fromAddress": "a@example.com"},
                         "recipients": [],
                         "attachments": [{"fileName": "", "sha256": "not-a-hash"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(org.hamcrest.Matchers.contains(
                        "attachments[0].fileName", "attachments[0].sha256", "recipients")));
    }

    private interface Request {
        void run() throws Exception;
    }

    private long statementsFor(Request request) throws Exception {
        statistics.clear();
        request.run();
        return statistics.getPrepareStatementCount();
    }

    private long create(String json) throws Exception {
        String body = mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }

    private static String alertWithChildren(int n) {
        String recipients = IntStream.range(0, n)
                .mapToObj(i -> "{\"address\": \"user%d@corp.example.com\", \"delivery\": \"DELIVERED\"}".formatted(i))
                .collect(Collectors.joining(","));
        String urls = IntStream.range(0, n)
                .mapToObj(i -> "{\"address\": \"https://site%d.example.net/login\"}".formatted(i))
                .collect(Collectors.joining(","));
        String clicks = IntStream.range(0, n)
                .mapToObj(i -> ("{\"recipientAddress\": \"user%d@corp.example.com\", "
                        + "\"urlAddress\": \"https://site%d.example.net/login\", "
                        + "\"clickedAt\": \"2026-10-28T14:05:00Z\", \"permitted\": true}").formatted(i, i))
                .collect(Collectors.joining(","));
        String attachments = IntStream.range(0, n)
                .mapToObj(i -> "{\"fileName\": \"file%d.pdf\"}".formatted(i))
                .collect(Collectors.joining(","));
        return """
                {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "DETECTED_PHISHING",
                 "priority": "LOW", "receivedAt": "2026-10-28T14:00:00Z",
                 "email": {"subject": "Fake", "fromAddress": "a@example.com"},
                 "recipients": [%s], "urls": [%s], "attachments": [%s], "clicks": [%s]}
                """.formatted(UUID.randomUUID(), recipients, urls, attachments, clicks);
    }

    private int rows(String table, long alertId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE alert_id = ?", Integer.class, alertId);
        return count == null ? 0 : count;
    }
}

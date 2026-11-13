package com.leul.havilor.alert;

import com.leul.havilor.FakeAiConfiguration;
import com.leul.havilor.FakeAiConfiguration.FakeAiClient;
import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import com.leul.havilor.alert.domain.enums.AuthResult;
import com.leul.havilor.alert.service.ExtractedEmail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 19: manual intake → a draft → the normal create flow (fake AiClient, no real API calls).
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class,
        FakeAiConfiguration.class})
class AlertIntakeIntegrationTest {

    private static final String EXTRACT = "/api/v1/alerts/manual/extract";
    private static final String TODAY = "MAN-20261101-"; // the fixed test clock's date

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeAiClient fakeAi;

    @BeforeEach
    void claudeFindsAnInvoicePhish() {
        fakeAi.answer(ExtractedEmail.class, () -> new ExtractedEmail("Invoice overdue", "billing@invoices.example.net",
                Optional.of("pay@invoices.example.org"), Optional.empty(), Optional.of(AuthResult.FAIL),
                Optional.empty(), Optional.of(AuthResult.FAIL), Optional.of(Instant.parse("2026-10-31T09:15:00Z")),
                List.of("alex@corp.example.com"), List.of("https://login.fake-bank.example.net/verify"),
                List.of("invoice.html")));
    }

    @AfterEach
    void deleteManualAlerts() {
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'MAN-%'");
    }

    @Test
    void pastedTextBecomesADraftThatTheNormalCreateFlowAccepts() throws Exception {
        String draft = mockMvc.perform(multipart(EXTRACT).param("text", "From: billing@invoices.example.net ..."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.ticketNumber").doesNotExist())
                .andExpect(jsonPath("$.caseType").value("USER_REPORTED_EMAIL"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.receivedAt").value("2026-10-31T09:15:00Z"))
                .andExpect(jsonPath("$.email.subject").value("Invoice overdue"))
                .andExpect(jsonPath("$.email.replyTo").value("pay@invoices.example.org"))
                .andExpect(jsonPath("$.email.spfResult").value("FAIL"))
                .andExpect(jsonPath("$.email.dkimResult").doesNotExist())
                .andExpect(jsonPath("$.recipients[0].address").value("alex@corp.example.com"))
                .andExpect(jsonPath("$.urls[0].address").value("https://login.fake-bank.example.net/verify"))
                .andExpect(jsonPath("$.attachments[0].fileName").value("invoice.html"))
                .andReturn().getResponse().getContentAsString();
        assertThat(fakeAi.lastInput.text()).contains("pasted", "From: billing@invoices.example.net");
        assertThat(fakeAi.lastInput.hasFile()).isFalse();
        // Nothing was saved by the extraction.
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alert WHERE ticket_number LIKE 'MAN-%'",
                Integer.class)).isZero();

        // The analyst sends the draft as it is: the existing create flow numbers it.
        create(draft).andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value(TODAY + "1"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
        create(draft).andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value(TODAY + "2"));
    }

    @Test
    void anEmlFileIsReadAsText() throws Exception {
        MockMultipartFile eml = new MockMultipartFile("file", "reported.EML", "application/octet-stream",
                "Subject: Invoice overdue\r\nFrom: billing@invoices.example.net\r\n\r\nPay now".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(EXTRACT).file(eml)).andExpect(status().isOk());

        assertThat(fakeAi.lastInput.hasFile()).isFalse();
        assertThat(fakeAi.lastInput.text()).contains(".eml file", "Subject: Invoice overdue", "Pay now");
    }

    @Test
    void anImageOrAPdfGoesToClaudesVision() throws Exception {
        mockMvc.perform(multipart(EXTRACT).file(new MockMultipartFile("file", "shot.png", "image/png", new byte[]{1, 2})))
                .andExpect(status().isOk());
        assertThat(fakeAi.lastInput.files().getFirst().mediaType()).isEqualTo("image/png");
        assertThat(fakeAi.lastInput.files().getFirst().data()).containsExactly(1, 2);

        mockMvc.perform(multipart(EXTRACT).file(new MockMultipartFile("file", "mail.pdf", "application/pdf", new byte[]{3})))
                .andExpect(status().isOk());
        assertThat(fakeAi.lastInput.files().getFirst().mediaType()).isEqualTo("application/pdf");
    }

    @Test
    void mistakesGive400() throws Exception {
        mockMvc.perform(multipart(EXTRACT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("input"));
        mockMvc.perform(multipart(EXTRACT).file(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}))
                        .param("text", "and some text"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("send exactly one: a 'text' part or a 'file' part"));
        mockMvc.perform(multipart(EXTRACT).file(new MockMultipartFile("file", "a.zip", "application/zip", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("file"));
    }

    @Test
    void onlyAManualAlertMayComeWithoutATicketNumber() throws Exception {
        create("""
                {"source": "SERVICENOW", "caseType": "USER_REPORTED_EMAIL", "priority": "MEDIUM",
                 "receivedAt": "2026-10-28T14:00:00Z",
                 "email": {"subject": "Fake", "fromAddress": "billing@invoices.example.net"},
                 "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("ticketNumber"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

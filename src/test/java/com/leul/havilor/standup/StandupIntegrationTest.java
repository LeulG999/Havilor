package com.leul.havilor.standup;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestAuthConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 17: standup notes. The default token is the test analyst's; the manager's is sent explicitly.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class StandupIntegrationTest {

    private static final String NOTES = "/api/v1/standup/notes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM standup_note WHERE text LIKE 'IT-NOTE%'");
        jdbcTemplate.update("DELETE FROM alert WHERE ticket_number LIKE 'INC-IT-%'");
    }

    @Test
    void writeEditShareAndTheManagerSeesIt() throws Exception {
        long a1 = createAlert();
        long a2 = createAlert();

        long id = idOf(create("{\"noteDate\": \"2026-11-02\", \"text\": \"IT-NOTE worked the invoice wave\", \"alertIds\": [%d]}"
                .formatted(a1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.authorId").value(testUsers.analystId()))
                .andExpect(jsonPath("$.alertIds").value(contains((int) a1))));

        // A draft is the author's alone: not on the team view yet.
        team().andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) id))));

        update(id, "{\"text\": \"IT-NOTE worked the invoice wave, 2 escalated\", \"alertIds\": [%d, %d]}".formatted(a2, a1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteDate").value("2026-11-02")) // not sent, so unchanged
                .andExpect(jsonPath("$.alertIds").value(contains((int) a1, (int) a2)));

        mockMvc.perform(post(NOTES + "/{id}/share", id)).andExpect(jsonPath("$.status").value("SHARED"));
        mockMvc.perform(post(NOTES + "/{id}/share", id)).andExpect(status().isOk()); // sharing twice is harmless

        team().andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(hasItem((int) id)))
                .andExpect(jsonPath("$.content[*].status").value(everyItem(is("SHARED"))));

        // Shared notes are final.
        update(id, "{\"text\": \"IT-NOTE changed later\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Note " + id + " is shared and can't be changed"));

        mockMvc.perform(get(NOTES)).andExpect(jsonPath("$.content[*].id").value(hasItem((int) id)));
    }

    @Test
    void notesArePrivateToTheirAuthor() throws Exception {
        long managersNote = idOf(mockMvc.perform(post(NOTES).header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteDate\": \"2026-11-02\", \"text\": \"IT-NOTE manager's own\"}"))
                .andExpect(status().isCreated()));

        // The analyst can't see, change or share it: 404, its existence is not revealed.
        mockMvc.perform(get(NOTES)).andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) managersNote))));
        update(managersNote, "{\"text\": \"IT-NOTE hijacked\"}").andExpect(status().isNotFound());
        mockMvc.perform(post(NOTES + "/{id}/share", managersNote)).andExpect(status().isNotFound());
    }

    @Test
    void onlyManagersSeeTheTeamView() throws Exception {
        mockMvc.perform(get("/api/v1/standup/team"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your role is not allowed to do this"));
    }

    @Test
    void mistakesGive400() throws Exception {
        create("{\"text\": \" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field").value(contains("noteDate", "text")));
        create("{\"noteDate\": \"2026-11-02\", \"text\": \"IT-NOTE x\", \"alertIds\": [%d]}".formatted(Long.MAX_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("alertIds"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("no alert with id " + Long.MAX_VALUE));
    }

    private ResultActions team() throws Exception {
        return mockMvc.perform(get("/api/v1/standup/team").param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()));
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post(NOTES).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long id, String json) throws Exception {
        return mockMvc.perform(patch(NOTES + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1"));
    }

    private long createAlert() throws Exception {
        return idOf(mockMvc.perform(post("/api/v1/alerts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticketNumber": "INC-IT-%s", "source": "TEST_GENERATOR", "caseType": "USER_REPORTED_EMAIL",
                         "priority": "MEDIUM", "receivedAt": "2026-10-28T14:00:00Z",
                         "email": {"subject": "Fake invoice overdue", "fromAddress": "billing@invoices.example.net"},
                         "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}]}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated()));
    }
}

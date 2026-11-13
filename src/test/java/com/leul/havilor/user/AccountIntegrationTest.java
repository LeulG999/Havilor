package com.leul.havilor.user;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow F30: the manager creates people (temporary password, shown once), turns accounts off and on, changes roles;
// a new person must set their own password before anything else works.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class, TestAuthConfiguration.class})
class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAuthConfiguration.TestUsers testUsers;

    @AfterEach
    void deleteTestUsers() {
        jdbcTemplate.update("DELETE FROM app_user WHERE email LIKE 'it-user-%'");
    }

    @Test
    void aNewPersonGetsATemporaryPasswordOnceAndMustReplaceIt() throws Exception {
        String email = "it-user-" + UUID.randomUUID() + "@havilor.example.com";
        String created = asManager(post("/api/v1/users"),
                "{\"name\": \"Sam Example\", \"email\": \"%s\", \"role\": \"ANALYST\"}".formatted(email.toUpperCase()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email)) // stored in lower case
                .andExpect(jsonPath("$.user.role").value("ANALYST"))
                .andExpect(jsonPath("$.user.active").value(true))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String temporary = JsonPath.read(created, "$.temporaryPassword");
        long id = ((Number) JsonPath.read(created, "$.user.id")).longValue();
        assertThat(temporary).hasSizeGreaterThanOrEqualTo(16).doesNotContainAnyWhitespaces();

        // Only a BCrypt hash is stored; no endpoint gives the password back.
        String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM app_user WHERE id = ?", String.class, id);
        assertThat(hash).startsWith("$2").doesNotContain(temporary);
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()))
                .andExpect(jsonPath("$[*].email").value(hasItem(email)))
                .andExpect(jsonPath("$[*].temporaryPassword").doesNotExist());

        // First login: the answer says so, and only /users/me and /users/me/password work.
        String login = login(email, temporary).andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(login, "$.accessToken");
        mockMvc.perform(get("/api/v1/alerts").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Set a new password first"));
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        // Wrong current password → 400; too short → 400; then the real change → a new token that works everywhere.
        json(post("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, token),
                "{\"currentPassword\": \"wrong-password\", \"newPassword\": \"a-long-enough-password\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));
        json(post("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, token),
                "{\"currentPassword\": \"%s\", \"newPassword\": \"too-short\"}".formatted(temporary))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
        String changed = json(post("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, token),
                "{\"currentPassword\": \"%s\", \"newPassword\": \"my-own-long-password\"}".formatted(temporary))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/v1/alerts").header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(changed, "$.accessToken")))
                .andExpect(status().isOk());

        login(email, temporary).andExpect(status().isUnauthorized()); // the temporary password is gone
        login(email, "my-own-long-password").andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        assertThat(actions(id)).containsExactly("USER_CREATED");
    }

    @Test
    void aManagerTurnsAccountsOffAndOnAndChangesRoles() throws Exception {
        String email = "it-user-" + UUID.randomUUID() + "@havilor.example.com";
        String created = asManager(post("/api/v1/users"),
                "{\"name\": \"Robin Example\", \"email\": \"%s\", \"role\": \"ANALYST\"}".formatted(email))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.user.id")).longValue();
        String temporary = JsonPath.read(created, "$.temporaryPassword");

        asManager(patch("/api/v1/users/{id}", id), "{\"active\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        login(email, temporary).andExpect(status().isUnauthorized()); // deactivated: no login
        asManager(patch("/api/v1/users/{id}", id), "{\"active\": true, \"role\": \"MANAGER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.role").value("MANAGER"));
        login(email, temporary).andExpect(status().isOk());

        assertThat(actions(id)).containsExactly("USER_CREATED", "USER_DEACTIVATED", "USER_REACTIVATED", "ROLE_CHANGED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT new_value FROM audit_entry WHERE entity_type = 'USER' AND entity_id = ? AND action = 'ROLE_CHANGED'",
                String.class, id)).isEqualTo("role=MANAGER");
    }

    @Test
    void theRulesAroundAccounts() throws Exception {
        // A manager can't turn off their own account.
        asManager(patch("/api/v1/users/{id}", testUsers.managerId()), "{\"active\": false}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You can't deactivate your own account"));
        // An email can be used only once (in any case).
        asManager(post("/api/v1/users"), "{\"name\": \"Copy\", \"email\": \"%s\", \"role\": \"ANALYST\"}"
                .formatted(TestAuthConfiguration.ANALYST_EMAIL.toUpperCase()))
                .andExpect(status().isConflict());
        // Bad input → 400; unknown user → 404.
        asManager(post("/api/v1/users"), "{\"name\": \"\", \"email\": \"not-an-email\"}")
                .andExpect(status().isBadRequest());
        asManager(patch("/api/v1/users/{id}", Long.MAX_VALUE), "{\"active\": true}").andExpect(status().isNotFound());
        // Analysts can't manage accounts (the default token is the analyst's).
        json(post("/api/v1/users"), "{\"name\": \"X\", \"email\": \"it-user-x@havilor.example.com\", \"role\": \"ANALYST\"}")
                .andExpect(status().isForbidden());
        json(patch("/api/v1/users/{id}", testUsers.managerId()), "{\"active\": false}").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
    }

    private List<String> actions(long userId) {
        return jdbcTemplate.queryForList(
                "SELECT action FROM audit_entry WHERE entity_type = 'USER' AND entity_id = ? ORDER BY id", String.class, userId);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, "")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private ResultActions asManager(MockHttpServletRequestBuilder request, String body) throws Exception {
        return json(request.header(HttpHeaders.AUTHORIZATION, testUsers.managerBearer()), body);
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }
}

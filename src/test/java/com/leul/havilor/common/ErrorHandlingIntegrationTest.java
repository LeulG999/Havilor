package com.leul.havilor.common;

import com.leul.havilor.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every kind of mistake comes back in the same ApiError shape.
// ThrowingController is imported only here, so its test endpoints exist in no other context.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ErrorHandlingIntegrationTest.ThrowingController.class})
class ErrorHandlingIntegrationTest {

    private static final String ALERTS = "/api/v1/alerts";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidFieldsGive400WithFieldErrors() throws Exception {
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketNumber": "  ", "email": {"subject": "%s", "phishScore": 101}}
                                """.formatted("x".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(ALERTS))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors", hasSize(9)))
                .andExpect(jsonPath("$.fieldErrors[*].field", contains(
                        "caseType", "email.fromAddress", "email.phishScore", "email.subject",
                        "priority", "receivedAt", "recipients", "source", "ticketNumber")));
    }

    @Test
    void unknownEnumValueGives400NamingTheField() throws Exception {
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketNumber": "INC-X", "priority": "URGENT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body has an unreadable value"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priority"));
    }

    @Test
    void malformedJsonGives400() throws Exception {
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void unreadableValueGives400NamingTheField() throws Exception {
        mockMvc.perform(post(ALERTS).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketNumber": "INC-X", "subject": "Fake", "receivedAt": "2026-10-32T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("receivedAt"));
    }

    @Test
    void wrongPathTypeGives400() throws Exception {
        mockMvc.perform(get(ALERTS + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be a Long"));
    }

    @Test
    void unknownAlertGives404WithBody() throws Exception {
        mockMvc.perform(get(ALERTS + "/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Alert " + Long.MAX_VALUE + " not found"))
                .andExpect(jsonPath("$.path").value(ALERTS + "/" + Long.MAX_VALUE));
    }

    @Test
    void unsupportedMethodKeepsSpringsStatus() throws Exception {
        mockMvc.perform(delete(ALERTS))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void conflictGives409() throws Exception {
        mockMvc.perform(get("/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Alert is closed"));
    }

    @Test
    void unexpectedErrorGives500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test-errors/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(content().string(not(containsString("secret-db-password"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    // Only exists in tests: gives us endpoints that throw, so 409 and 500 can be checked end to end.
    @RestController
    static class ThrowingController {

        @GetMapping("/test-errors/conflict")
        void conflict() {
            throw new ConflictException("Alert is closed");
        }

        @GetMapping("/test-errors/boom")
        void boom() {
            throw new IllegalStateException("connection failed for secret-db-password");
        }
    }
}

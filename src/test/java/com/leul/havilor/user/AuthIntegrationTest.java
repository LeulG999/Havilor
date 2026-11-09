package com.leul.havilor.user;

import com.leul.havilor.FixedClockConfiguration;
import com.leul.havilor.TestcontainersConfiguration;
import com.leul.havilor.user.domain.User;
import com.leul.havilor.user.domain.enums.Role;
import com.leul.havilor.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.leul.havilor.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Flow 12: login and tokens, end to end. This test does NOT import TestAuthConfiguration, so MockMvc sends no
// token unless a test adds one: that is how "no token → 401" is checked.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class AuthIntegrationTest {

    private static final String EMAIL = "auth.test.analyst@havilor.example.com";
    private static final String INACTIVE_EMAIL = "auth.test.inactive@havilor.example.com";
    private static final String PASSWORD = "auth-test-only-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createUsers() {
        if (userRepository.findByEmail(EMAIL).isEmpty()) {
            userRepository.save(new User("Auth Test Analyst", EMAIL, passwordEncoder.encode(PASSWORD), Role.ANALYST));
        }
        if (userRepository.findByEmail(INACTIVE_EMAIL).isEmpty()) {
            userRepository.save(new User("Auth Test Inactive", INACTIVE_EMAIL, passwordEncoder.encode(PASSWORD),
                    Role.ANALYST));
            jdbcTemplate.update("UPDATE app_user SET active = FALSE WHERE email = ?", INACTIVE_EMAIL);
        }
    }

    @Test
    void loginGivesATokenThatOpensTheApi() throws Exception {
        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(Duration.ofHours(8).toSeconds()));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("Auth Test Analyst"))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void emailIsNotCaseSensitive() throws Exception {
        login("  Auth.Test.Analyst@HAVILOR.example.com ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void everyFailedLoginGivesTheSame401() throws Exception {
        for (String[] attempt : new String[][]{
                {EMAIL, "wrong-password"}, {"nobody@havilor.example.com", PASSWORD}, {INACTIVE_EMAIL, PASSWORD}}) {
            login(attempt[0], attempt[1])
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Test
    void loginNeedsEmailAndPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void passwordsAreStoredAsBcryptHashes() {
        String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM app_user WHERE email = ?", String.class, EMAIL);
        assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
    }

    @Test
    void theApiNeedsAValidToken() throws Exception {
        // no token at all
        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A valid access token is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/alerts"));
        // not a JWT
        bearer("not-a-real-token").andExpect(status().isUnauthorized());
        // signed with another key
        JwtEncoder otherKey = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
                new SecretKeySpec("a-different-secret-that-is-long-enough-1234".getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256")));
        bearer(sign(otherKey, NOW.plusSeconds(60))).andExpect(status().isUnauthorized());
        // our key, but expired (the test clock says NOW; the validator allows 60 seconds of clock skew)
        bearer(sign(jwtEncoder, NOW.minusSeconds(120))).andExpect(status().isUnauthorized());
        // our key, still valid
        bearer(sign(jwtEncoder, NOW.plusSeconds(60))).andExpect(status().isOk());
    }

    private ResultActions bearer(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/alerts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String sign(JwtEncoder encoder, java.time.Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1").issuedAt(expiresAt.minusSeconds(3600)).expiresAt(expiresAt).claim("role", "ANALYST").build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private String tokenFor(String email) throws Exception {
        String body = login(email, PASSWORD).andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}

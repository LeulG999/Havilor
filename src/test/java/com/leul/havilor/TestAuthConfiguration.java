package com.leul.havilor;

import com.leul.havilor.user.api.request.LoginRequest;
import com.leul.havilor.user.domain.User;
import com.leul.havilor.user.domain.enums.Role;
import com.leul.havilor.user.repository.UserRepository;
import com.leul.havilor.user.service.AuthService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Every API test imports this. It makes sure one fake analyst and one fake manager exist, logs both in through
// the real AuthService, and sends the analyst's token with EVERY MockMvc request by default.
// A test can still send another "Authorization" header (e.g. the manager's token): the request's own header wins.
@TestConfiguration(proxyBeanMethods = false)
public class TestAuthConfiguration {

    public static final String ANALYST_EMAIL = "test.analyst@havilor.example.com";
    public static final String MANAGER_EMAIL = "test.manager@havilor.example.com";
    public static final String PASSWORD = "test-only-password";

    // The ids and tokens of the two test users; autowire it in a test that needs them.
    public record TestUsers(Long analystId, String analystToken, Long managerId, String managerToken) {

        public String analystBearer() {
            return "Bearer " + analystToken;
        }

        public String managerBearer() {
            return "Bearer " + managerToken;
        }
    }

    @Bean
    TestUsers testUsers(UserRepository users, PasswordEncoder passwordEncoder, AuthService authService) {
        // The test database is shared by all test contexts in one run: create each user only once.
        Long analystId = findOrCreate(users, passwordEncoder, "Test Analyst", ANALYST_EMAIL, Role.ANALYST);
        Long managerId = findOrCreate(users, passwordEncoder, "Test Manager", MANAGER_EMAIL, Role.MANAGER);
        return new TestUsers(
                analystId, authService.login(new LoginRequest(ANALYST_EMAIL, PASSWORD)).accessToken(),
                managerId, authService.login(new LoginRequest(MANAGER_EMAIL, PASSWORD)).accessToken());
    }

    @Bean
    MockMvcBuilderCustomizer analystTokenByDefault(TestUsers testUsers) {
        return builder -> builder.defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, testUsers.analystBearer()));
    }

    private static Long findOrCreate(UserRepository users, PasswordEncoder passwordEncoder,
                                     String name, String email, Role role) {
        return users.findByEmail(email)
                .orElseGet(() -> users.save(new User(name, email, passwordEncoder.encode(PASSWORD), role)))
                .getId();
    }
}

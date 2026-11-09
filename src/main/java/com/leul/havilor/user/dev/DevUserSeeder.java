package com.leul.havilor.user.dev;

import com.leul.havilor.user.domain.User;
import com.leul.havilor.user.domain.enums.Role;
import com.leul.havilor.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Dev profile only: one fake analyst and one fake manager to log in with. Both use the password from the
// HAVILOR_DEV_PASSWORD environment variable, so no password is ever written in the code or in git.
// Users that already exist are left alone.
@Component
@Profile("dev")
class DevUserSeeder implements ApplicationRunner {

    static final String ANALYST_EMAIL = "analyst@havilor.example.com";
    static final String MANAGER_EMAIL = "manager@havilor.example.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    DevUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                  @Value("${havilor.dev.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password.isBlank()) {
            throw new IllegalStateException("The dev profile needs HAVILOR_DEV_PASSWORD to create the dev users");
        }
        createIfMissing("Dana Analyst", ANALYST_EMAIL, Role.ANALYST);
        createIfMissing("Morgan Manager", MANAGER_EMAIL, Role.MANAGER);
    }

    private void createIfMissing(String name, String email, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(new User(name, email, passwordEncoder.encode(password), role));
        }
    }
}

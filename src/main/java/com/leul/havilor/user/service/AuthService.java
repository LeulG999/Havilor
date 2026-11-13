package com.leul.havilor.user.service;

import com.leul.havilor.common.error.UnauthorizedException;
import com.leul.havilor.common.error.BadRequestException;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.user.api.request.ChangePasswordRequest;
import com.leul.havilor.user.api.request.LoginRequest;
import com.leul.havilor.user.api.response.LoginResponse;
import com.leul.havilor.user.domain.User;
import com.leul.havilor.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// Login: check the password, then hand out one signed access token. No refresh tokens: when it expires,
// the analyst logs in again.
@Service
public class AuthService {

    // One analyst shift.
    static final Duration TOKEN_LIFETIME = Duration.ofHours(8);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email()))
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return tokenFor(user);
    }

    // The person's own password (Flow F30). Wrong current password → 400. The answer is a NEW token (without the
    // mustChangePassword claim), so after a first login the person can use Havilor right away.
    @Transactional
    public LoginResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("currentPassword", "is not correct");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        return tokenFor(user);
    }

    // One signed access token. subject = user id (what the services need); role = what SecurityConfiguration turns
    // into ROLE_...; mustChangePassword = what PasswordChangeFilter checks (Flow F30), so no database lookup is needed
    // on every request. After a password change the person gets a new token without it.
    LoginResponse tokenFor(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("havilor")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .claim("role", user.getRole().name())
                .claim("mustChangePassword", user.isMustChangePassword())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds(), user.isMustChangePassword());
    }
}

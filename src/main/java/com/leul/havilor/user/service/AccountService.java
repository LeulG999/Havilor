package com.leul.havilor.user.service;

import com.leul.havilor.audit.service.AuditService;
import com.leul.havilor.common.error.ConflictException;
import com.leul.havilor.common.error.NotFoundException;
import com.leul.havilor.user.api.request.CreateUserRequest;
import com.leul.havilor.user.api.request.UpdateUserRequest;
import com.leul.havilor.user.api.response.CreatedUserResponse;
import com.leul.havilor.user.api.response.UserResponse;
import com.leul.havilor.user.domain.User;
import com.leul.havilor.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

// Account management (Flow F30): the manager acts as admin. Creates people with a temporary password, turns
// accounts off and on, changes roles. Every change is in the audit log (entity type USER).
@Service
public class AccountService {

    // No look-alike characters (0/O, 1/l/I), so the password can be read out or typed without mistakes.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    static final int TEMPORARY_PASSWORD_LENGTH = 20;
    private static final SecureRandom RANDOM = new SecureRandom(); // a cryptographically strong random generator

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    // Everybody, by name, including deactivated accounts (the Team page).
    @Transactional(readOnly = true)
    public List<UserResponse> all() {
        return userRepository.findAll(Sort.by("name", "id")).stream().map(UserResponse::from).toList();
    }

    // Email already used → 409. The temporary password goes back ONCE; only its BCrypt hash is stored.
    @Transactional
    public CreatedUserResponse create(CreateUserRequest request, Long actorId) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("A user with the email " + email + " already exists");
        }
        String temporaryPassword = temporaryPassword();
        User user = userRepository.save(User.withTemporaryPassword(request.name().trim(), email,
                passwordEncoder.encode(temporaryPassword), request.role()));
        auditService.record(actorId, "USER_CREATED", AuditService.USER, user.getId(), null,
                "name=" + user.getName() + ", email=" + user.getEmail() + ", role=" + user.getRole());
        return new CreatedUserResponse(UserResponse.from(user), temporaryPassword);
    }

    // Unknown user → 404; a manager turning off their own account → 409 (they would lock themselves out).
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, Long actorId) {
        User user = userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
        if (request.active() != null && !request.active() && id.equals(actorId)) {
            throw new ConflictException("You can't deactivate your own account");
        }
        if (request.active() != null && request.active() != user.isActive()) {
            auditService.record(actorId, request.active() ? "USER_REACTIVATED" : "USER_DEACTIVATED",
                    AuditService.USER, id, "active=" + user.isActive(), "active=" + request.active());
            user.changeActive(request.active());
        }
        if (request.role() != null && request.role() != user.getRole()) {
            auditService.record(actorId, "ROLE_CHANGED", AuditService.USER, id,
                    "role=" + user.getRole(), "role=" + request.role());
            user.changeRole(request.role());
        }
        return UserResponse.from(user);
    }

    static String temporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}

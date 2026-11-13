package com.leul.havilor.user.domain;

import com.leul.havilor.user.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    // True after a manager created the account with a temporary password (Flow F30): until the person sets their
    // own, the API only lets them do that.
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    // passwordHash must already be hashed (BCrypt). A new user is active.
    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
    }

    // Created by a manager: a temporary password (already hashed) that must be replaced at the first login.
    public static User withTemporaryPassword(String name, String email, String temporaryPasswordHash, Role role) {
        User user = new User(name, email, temporaryPasswordHash, role);
        user.mustChangePassword = true;
        return user;
    }

    // The person's own new password (already hashed): the temporary one is gone.
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

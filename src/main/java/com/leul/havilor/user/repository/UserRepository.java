package com.leul.havilor.user.repository;

import com.leul.havilor.user.domain.User;
import com.leul.havilor.user.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Pass a normalized (lower-case) email: see User.normalizeEmail.
    Optional<User> findByEmail(String email);

    List<User> findByRoleAndActiveTrueOrderByName(Role role);
}

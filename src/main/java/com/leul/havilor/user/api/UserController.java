package com.leul.havilor.user.api;

import com.leul.havilor.user.api.response.UserResponse;
import com.leul.havilor.user.domain.enums.Role;
import com.leul.havilor.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Who am I? The token's subject is the user id.
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return userService.getById(Long.valueOf(token.getSubject()));
    }

    // MANAGER only (rule in SecurityConfiguration): active users with a role, e.g. ?role=ANALYST for the
    // "Assign to…" list. An unknown role → 400.
    @GetMapping
    public List<UserResponse> byRole(@RequestParam Role role) {
        return userService.activeByRole(role);
    }
}

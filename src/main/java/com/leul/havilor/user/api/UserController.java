package com.leul.havilor.user.api;

import com.leul.havilor.user.api.request.ChangePasswordRequest;
import com.leul.havilor.user.api.request.CreateUserRequest;
import com.leul.havilor.user.api.request.UpdateUserRequest;
import com.leul.havilor.user.api.response.CreatedUserResponse;
import com.leul.havilor.user.api.response.LoginResponse;
import com.leul.havilor.user.api.response.UserResponse;
import com.leul.havilor.user.domain.enums.Role;
import com.leul.havilor.user.service.AccountService;
import com.leul.havilor.user.service.AuthService;
import com.leul.havilor.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AccountService accountService;
    private final AuthService authService;

    public UserController(UserService userService, AccountService accountService, AuthService authService) {
        this.userService = userService;
        this.accountService = accountService;
        this.authService = authService;
    }

    // Who am I? The token's subject is the user id.
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return userService.getById(Long.valueOf(token.getSubject()));
    }

    // MANAGER only (rule in SecurityConfiguration). ?role=ANALYST: the active users with that role (the "Assign to…"
    // list); no role: everybody, also deactivated accounts (the Team page). An unknown role → 400.
    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return role == null ? accountService.all() : userService.activeByRole(role);
    }

    // MANAGER only: a new person with a temporary password, shown once in the answer. Email taken → 409.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedUserResponse create(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal Jwt token) {
        return accountService.create(request, Long.valueOf(token.getSubject()));
    }

    // MANAGER only: turn an account off / on, change the role. Deactivating yourself → 409.
    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                               @AuthenticationPrincipal Jwt token) {
        return accountService.update(id, request, Long.valueOf(token.getSubject()));
    }

    // Everyone: set your own password (the only thing allowed while it is still a temporary one). Returns a new token.
    @PostMapping("/me/password")
    public LoginResponse changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                        @AuthenticationPrincipal Jwt token) {
        return authService.changePassword(Long.valueOf(token.getSubject()), request);
    }
}

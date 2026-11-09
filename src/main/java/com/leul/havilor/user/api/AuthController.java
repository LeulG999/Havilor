package com.leul.havilor.user.api;

import com.leul.havilor.user.api.request.LoginRequest;
import com.leul.havilor.user.api.response.LoginResponse;
import com.leul.havilor.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The only /api/v1 endpoint that works without a token (see SecurityConfiguration).
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 200 with a token; 401 for a wrong email/password or an inactive user.
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}

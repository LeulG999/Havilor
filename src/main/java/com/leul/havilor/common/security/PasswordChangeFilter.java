package com.leul.havilor.common.security;

import com.leul.havilor.common.error.PasswordChangeRequiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

// Flow F30: someone logged in with a temporary password may only look at themselves and set a new password.
// It reads the token's "mustChangePassword" claim (set at login), so it needs no database lookup. Any other
// request gets 403 "Set a new password first", through the same error handler as every other error.
// Not a @Component on purpose: SecurityConfiguration adds it to the security chain, right after the token check.
class PasswordChangeFilter extends OncePerRequestFilter {

    private static final String ME = "/api/v1/users/me";
    private static final String MY_PASSWORD = "/api/v1/users/me/password";

    private final HandlerExceptionResolver resolver;

    PasswordChangeFilter(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean mustChange = authentication instanceof JwtAuthenticationToken jwt
                && Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean("mustChangePassword"));
        String path = request.getRequestURI();
        if (mustChange && path.startsWith("/api/") && !path.equals(ME) && !path.equals(MY_PASSWORD)) {
            resolver.resolveException(request, response, null, new PasswordChangeRequiredException());
            return; // the request stops here
        }
        chain.doFilter(request, response);
    }
}

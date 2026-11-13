package com.leul.havilor.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

// Who may call what. Every /api/v1 endpoint needs a valid access token (a JWT we signed ourselves),
// except the login itself; /actuator/health is open, every other /actuator path is refused. Manager-only URLs are listed here too, so all access rules are in one place.
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        // 401 and 403 go through GlobalExceptionHandler like every other error, so they have the same ApiError body.
        AuthenticationEntryPoint unauthenticated = (request, response, e) -> resolver.resolveException(request, response, null, e);
        AccessDeniedHandler forbidden = (request, response, e) -> resolver.resolveException(request, response, null, e);

        http
                // Stateless API: the token travels in the Authorization header, not in a cookie,
                // so there is no session and no CSRF to protect against.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // The health check is for Docker, load balancers and CI: no token, no details (Flow 23).
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/standup/team").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("MANAGER")
                        // The investigation itself is the analyst's work (Flow F25): steps, findings, actions, the AI
                        // assistant, escalations and closing. Managers read everything and may change priority and
                        // assignment (the assignment rules are in AlertService.update).
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/alerts/*/steps/*", "/api/v1/escalations/*")
                        .hasRole("ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/v1/alerts/*/findings", "/api/v1/alerts/*/actions",
                                "/api/v1/alerts/*/ai/**", "/api/v1/alerts/*/escalations", "/api/v1/alerts/*/close")
                        .hasRole("ANALYST")
                        .requestMatchers("/api/v1/**").authenticated()
                        // Nothing else is served; unknown paths still get their normal 404.
                        .anyRequest().permitAll())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromTokenClaim()))
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(forbidden));
        return http.build();
    }

    // The token's "role" claim (ANALYST or MANAGER) becomes the authority ROLE_ANALYST / ROLE_MANAGER.
    private static JwtAuthenticationConverter rolesFromTokenClaim() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    // One shared secret signs (encoder) and checks (decoder) tokens: HMAC-SHA256.
    @Bean
    SecretKey jwtSigningKey(@Value("${havilor.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("havilor.jwt.secret (env HAVILOR_JWT_SECRET) must be at least 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    // Expiry is checked against the app Clock, like every other "now" in Havilor.
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator notExpired = new JwtTimestampValidator();
        notExpired.setClock(clock);
        decoder.setJwtValidator(notExpired);
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

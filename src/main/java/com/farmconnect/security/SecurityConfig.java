package com.farmconnect.security;

import com.farmconnect.service.AuditService;
import com.farmconnect.service.LoginGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;
    private final AuditService audit;
    private final LoginGuard guard;
    /** The H2 database console is a back door into every table. Off unless someone switches it on for local development. */
    @Value("${spring.h2.console.enabled:false}") private boolean h2Console;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/google",
                                 "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/h2-console/**").access((authn, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(h2Console))
                // IMPORTANT: Spring forwards every 4xx/5xx to /error. If /error needs a login, a normal
                // "400 Please provide..." or "409 ..." gets turned into 401 and the app says "login again".
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ussd", "/api/sms/inbound").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/callback/**", "/api/payments/b2c/**", "/api/payments/c2b/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/officer", "/officer/", "/officer/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/company", "/company/", "/company/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/files/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/farms/mine", "/api/products/mine").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/farms/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(h -> {
                if (h2Console) h.frameOptions(f -> f.sameOrigin()); else h.frameOptions(f -> f.deny());
                h.httpStrictTransportSecurity(s -> s.maxAgeInSeconds(31_536_000).includeSubDomains(true));
                h.referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER));
                h.contentTypeOptions(c -> { });
            })
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AuditFilter(audit, guard), JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

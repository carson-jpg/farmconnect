package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.model.PasswordResetCode;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.PasswordResetCodeRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.JwtService;
import com.farmconnect.service.GoogleAuthService;
import com.farmconnect.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final PasswordResetCodeRepository resets;
    private final MailService mail;
    private final GoogleAuthService google;

    private static final SecureRandom RNG = new SecureRandom();
    private static final int RESET_MINUTES = 15;
    private static final int RESET_MAX_ATTEMPTS = 5;
    private static final int RESEND_SECONDS = 60;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        // Only farmers and buyers can self-register. ADMIN is seeded, OFFICER is created by an admin.
        if (r.role() != Role.FARMER && r.role() != Role.BUYER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This role cannot be self-registered");
        if (r.confirmPassword() != null && !r.confirmPassword().equals(r.password()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        if (users.existsByEmail(r.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setPhone(r.phone());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(r.role());
        users.save(u);
        return toResponse(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!u.isEnabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been suspended. Please contact FarmConnect support.");
        return toResponse(u);
    }

    /** Always answers the same way, so nobody can use it to find out which emails have an account. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotRequest r) {
        String email = r.email().trim();
        users.findByEmail(email).filter(User::isEnabled).ifPresent(u -> {
            PasswordResetCode rc = resets.findByEmail(email).orElseGet(PasswordResetCode::new);
            Instant now = Instant.now();
            if (rc.getId() != null && rc.getCreatedAt().isAfter(now.minusSeconds(RESEND_SECONDS))) return; // too soon to resend
            String code = String.format("%06d", RNG.nextInt(1_000_000));
            rc.setEmail(email);
            rc.setCodeHash(encoder.encode(code));
            rc.setCreatedAt(now);
            rc.setExpiresAt(now.plusSeconds(RESET_MINUTES * 60L));
            rc.setAttempts(0);
            resets.save(rc);
            try {
                mail.send(email, "Your FarmConnect password reset code",
                        "Your FarmConnect password reset code is " + code + ".\n"
                        + "It expires in " + RESET_MINUTES + " minutes. If you did not ask for this, ignore this email.");
            } catch (Exception ignored) { /* logged in MailService; keep the answer identical */ }
        });
        return Map.of("message", "If that email is registered, a 6-digit code has been sent to it.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetRequest r) {
        String email = r.email().trim();
        PasswordResetCode rc = resets.findByEmail(email).orElseThrow(AuthController::badCode);
        if (rc.getExpiresAt().isBefore(Instant.now()) || rc.getAttempts() >= RESET_MAX_ATTEMPTS) {
            resets.delete(rc);
            throw badCode();
        }
        if (!encoder.matches(r.code().trim(), rc.getCodeHash())) {
            rc.setAttempts(rc.getAttempts() + 1);
            resets.save(rc);
            throw badCode();
        }
        User u = users.findByEmail(email).orElseThrow(AuthController::badCode);
        u.setPassword(encoder.encode(r.newPassword()));
        users.save(u);
        resets.delete(rc);
        return Map.of("message", "Password updated. You can now log in.");
    }

    /**
     * Google sign-in / sign-up. Existing account -> logs in. New email -> needs "role" (FARMER or BUYER);
     * without it the answer is 404 "NEW_ACCOUNT" and the app asks the user to pick one, then calls again.
     */
    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleRequest r) {
        GoogleAuthService.Identity g = google.verify(r.idToken());
        User u = users.findByEmailIgnoreCase(g.email()).orElse(null);
        if (u == null) {
            if (r.role() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NEW_ACCOUNT");
            if (r.role() != Role.FARMER && r.role() != Role.BUYER)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This role cannot be self-registered");
            u = new User();
            u.setName(g.name());
            u.setEmail(g.email());
            u.setPassword(encoder.encode(UUID.randomUUID().toString())); // unusable; they can set one via "Forgot password"
            u.setRole(r.role());
            users.save(u);
        }
        if (!u.isEnabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been suspended. Please contact FarmConnect support.");
        return toResponse(u);
    }

    private static ResponseStatusException badCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code");
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal User u) {
        return new AuthResponse(null, u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generate(u), u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
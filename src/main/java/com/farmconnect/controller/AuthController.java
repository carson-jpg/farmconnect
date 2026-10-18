package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.model.PasswordResetCode;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.PasswordResetCodeRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.ClientIp;
import com.farmconnect.security.JwtService;
import com.farmconnect.security.PasswordPolicy;
import com.farmconnect.service.AuditService;
import com.farmconnect.service.LoginGuard;
import jakarta.servlet.http.HttpServletRequest;
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
import java.time.Duration;
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
    private final LoginGuard guard;
    private final AuditService audit;
    /** A real bcrypt hash used so a wrong email takes as long as a wrong password (no account-guessing by timing). */
    private static final String DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("not-a-real-password");

    private static final SecureRandom RNG = new SecureRandom();
    private static final int RESET_MINUTES = 15;
    private static final int RESET_MAX_ATTEMPTS = 5;
    private static final int RESEND_SECONDS = 60;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r, HttpServletRequest http) {
        guard.limit("register", ClientIp.of(http), 100, Duration.ofHours(1));   // many farmers share one mobile-network address
        PasswordPolicy.require(r.password(), r.email());
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
    public AuthResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest http) {
        String ip = ClientIp.of(http);
        String email = r.email().trim();
        try {
            guard.check(email, ip);
        } catch (ResponseStatusException blocked) {
            audit.record(null, email, null, "LOGIN_BLOCKED", email, "Locked out after too many wrong passwords", ip, false);
            throw blocked;
        }
        User found = users.findByEmail(email).orElse(null);
        boolean ok = encoder.matches(r.password(), found == null ? DUMMY_HASH : found.getPassword()) && found != null;
        if (!ok) {
            boolean locked = guard.fail(email, ip);
            String why = found == null ? "No such account" : "Wrong password";
            audit.record(found == null ? null : found.getId(), email, found == null ? null : found.getRole().name(), "LOGIN_FAILED", email,
                    locked ? why + "; locked for 15 minutes" : why, ip, false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        User u = found;
        if (!u.isEnabled()) {
            audit.record(u, "LOGIN_REFUSED", email, "Suspended account", ip, false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been suspended. Please contact FarmConnect support.");
        }
        guard.success(email, ip);
        audit.record(u, "LOGIN_OK", email, null, ip, true);
        return toResponse(u);
    }

    /** Always answers the same way, so nobody can use it to find out which emails have an account. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotRequest r, HttpServletRequest http) {
        String email = r.email().trim();
        guard.limit("forgot-ip", ClientIp.of(http), 10, Duration.ofMinutes(15));
        guard.limit("forgot-email", email.toLowerCase(), 5, Duration.ofMinutes(15));
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
    public Map<String, String> resetPassword(@Valid @RequestBody ResetRequest r, HttpServletRequest http) {
        String email = r.email().trim();
        String ip = ClientIp.of(http);
        guard.limit("reset-ip", ip, 20, Duration.ofMinutes(15));
        PasswordPolicy.require(r.newPassword(), email);
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
        u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);   // every old session ends
        users.save(u);
        resets.delete(rc);
        audit.record(u, "PASSWORD_RESET", email, "Reset with an emailed code", ip, true);
        return Map.of("message", "Password updated. You can now log in.");
    }

    /**
     * Google sign-in / sign-up. Existing account -> logs in. New email -> needs "role" (FARMER or BUYER);
     * without it the answer is 404 "NEW_ACCOUNT" and the app asks the user to pick one, then calls again.
     */
    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleRequest r, HttpServletRequest http) {
        String ip = ClientIp.of(http);
        guard.limit("google-ip", ip, 40, Duration.ofMinutes(15));
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
        if (!u.isEnabled()) {
            audit.record(u, "LOGIN_REFUSED", g.email(), "Suspended account (Google)", ip, false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been suspended. Please contact FarmConnect support.");
        }
        audit.record(u, "LOGIN_OK", g.email(), "Google", ip, true);
        return toResponse(u);
    }

    public record ChangePasswordRequest(@jakarta.validation.constraints.NotBlank String currentPassword,
                                        @jakarta.validation.constraints.NotBlank String newPassword) {}

    /** Change my own password. Ends every other session and returns a fresh token for this one. */
    @PostMapping("/change-password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest r, @AuthenticationPrincipal User me, HttpServletRequest http) {
        String ip = ClientIp.of(http);
        guard.limit("change-password", String.valueOf(me.getId()), 10, Duration.ofMinutes(15));
        User u = users.findById(me.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again"));
        if (!encoder.matches(r.currentPassword(), u.getPassword())) {
            audit.record(u, "PASSWORD_CHANGE_FAILED", u.getEmail(), "Wrong current password", ip, false);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is not correct");
        }
        PasswordPolicy.require(r.newPassword(), u.getEmail());
        u.setPassword(encoder.encode(r.newPassword()));
        u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
        users.save(u);
        audit.record(u, "PASSWORD_CHANGED", u.getEmail(), null, ip, true);
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
package com.farmconnect.controller;

import com.farmconnect.model.AuditLog;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.AuditLogRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.ClientIp;
import com.farmconnect.service.AuditService;
import com.farmconnect.service.LoginGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The super admin's security desk: see every sign-in and blocked attempt, clear lockouts, end a user's sessions,
 * and issue a new temporary password. Nobody else can reach any of this (class-level ADMIN only).
 */
@RestController
@RequestMapping("/api/admin/security")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SecurityAdminController {
    private final AuditLogRepository auditRepo;
    private final AuditService audit;
    private final LoginGuard guard;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    private static final SecureRandom RNG = new SecureRandom();
    static final String DEFAULT_ADMIN_PASSWORD = "ChangeMe123!";

    public record Summary(long failedLogins24h, long blocked24h, long denied24h, long adminLogins24h, int activeLocks,
                          boolean defaultAdminPassword) {}
    public record AuditEntry(Long id, Instant at, String actorEmail, String actorRole, String action, String target,
                             String detail, String ip, boolean success) {}
    public record LockInfo(String kind, String subject, Instant until) {}
    public record UnlockRequest(String email, String ip) {}
    public record TempPassword(String email, String temporaryPassword) {}

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Summary summary() {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        long adminLogins = auditRepo.findAllByOrderByIdDesc(PageRequest.of(0, 2000)).stream()
                .filter(a -> "LOGIN_OK".equals(a.getAction()) && a.getAt().isAfter(since) && ("ADMIN".equals(a.getActorRole()) || "COUNTY_ADMIN".equals(a.getActorRole())))
                .count();
        boolean weak = users.findAll().stream().anyMatch(u -> u.getRole() == Role.ADMIN && encoder.matches(DEFAULT_ADMIN_PASSWORD, u.getPassword()));
        return new Summary(auditRepo.countByActionAndAtAfter("LOGIN_FAILED", since), auditRepo.countByActionAndAtAfter("LOGIN_BLOCKED", since),
                auditRepo.countByActionAndAtAfter("ACCESS_DENIED", since), adminLogins, guard.activeLocks().size(), weak);
    }

    /** Newest first. {@code action} = exact action name (e.g. LOGIN_FAILED); {@code q} searches who, what and where from. */
    @GetMapping("/audit")
    @Transactional(readOnly = true)
    public List<AuditEntry> audit(@RequestParam(required = false) String action, @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "200") int limit) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        int max = Math.max(1, Math.min(limit, 500));
        return auditRepo.findAllByOrderByIdDesc(PageRequest.of(0, 3000)).stream()
                .filter(a -> action == null || action.isBlank() || action.equalsIgnoreCase(a.getAction()))
                .filter(a -> needle.isEmpty() || contains(a.getActorEmail(), needle) || contains(a.getTarget(), needle)
                        || contains(a.getIp(), needle) || contains(a.getDetail(), needle))
                .limit(max)
                .map(a -> new AuditEntry(a.getId(), a.getAt(), a.getActorEmail(), a.getActorRole(), a.getAction(), a.getTarget(),
                        a.getDetail(), a.getIp(), a.isSuccess())).toList();
    }

    @GetMapping("/locks")
    public List<LockInfo> locks() {
        return guard.activeLocks().stream().map(l -> new LockInfo(l.kind(), l.subject(), l.until())).toList();
    }

    /** Clear a lockout by email (all addresses) and/or by address. */
    @PostMapping("/locks/unlock")
    public Map<String, Integer> unlock(@RequestBody UnlockRequest r, @AuthenticationPrincipal User me, HttpServletRequest http) {
        int n = 0;
        if (r.email() != null && !r.email().isBlank()) n += guard.unlockAccount(r.email());
        if (r.ip() != null && !r.ip().isBlank()) n += guard.unlockAddress(r.ip().trim());
        audit.record(me, "UNLOCK", (r.email() == null ? "" : r.email()) + (r.ip() == null ? "" : " " + r.ip()), "Cleared " + n + " lock(s)", ClientIp.of(http), true);
        return Map.of("cleared", n);
    }

    /** End every session of a user: their apps and browsers must sign in again. */
    @PostMapping("/users/{id}/force-logout")
    @Transactional
    public Map<String, String> forceLogout(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
        users.save(u);
        audit.record(me, "FORCE_LOGOUT", u.getEmail(), "All sessions ended", ClientIp.of(http), true);
        return Map.of("message", "Signed out everywhere: " + u.getEmail());
    }

    /** Give someone a new random temporary password (shown once) and end all their sessions. For another super admin use "forgot password". */
    @PostMapping("/users/{id}/reset-password")
    @Transactional
    public TempPassword resetPassword(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A super admin changes their own password (Security > Change my password)");
        String temp = randomPassword();
        u.setPassword(encoder.encode(temp));
        u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
        users.save(u);
        guard.unlockAccount(u.getEmail());
        audit.record(me, "ADMIN_PASSWORD_RESET", u.getEmail(), "Temporary password issued; all sessions ended", ClientIp.of(http), true);
        return new TempPassword(u.getEmail(), temp);
    }

    private static boolean contains(String s, String needle) { return s != null && s.toLowerCase(Locale.ROOT).contains(needle); }

    /** 12 characters, no look-alikes, always with a letter and a number. */
    static String randomPassword() {
        String letters = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ", digits = "23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(letters.charAt(RNG.nextInt(letters.length())));
        sb.insert(RNG.nextInt(sb.length() + 1), digits.charAt(RNG.nextInt(digits.length())));
        sb.insert(RNG.nextInt(sb.length() + 1), digits.charAt(RNG.nextInt(digits.length())));
        return sb.toString();
    }
}

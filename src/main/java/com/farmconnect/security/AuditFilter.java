package com.farmconnect.security;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.service.AuditService;
import com.farmconnect.service.LoginGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Records, for the super admin: everything staff change (any POST/PUT/PATCH/DELETE by an admin, county admin or officer),
 * and every blocked attempt (403 for a logged-in user, 401 on admin or payment endpoints), so forced access shows up.
 * Passwords and request bodies are never logged, only who, what path, and the result.
 */
public class AuditFilter extends OncePerRequestFilter {
    private static final Set<String> READ = Set.of("GET", "HEAD", "OPTIONS");
    private final AuditService audit;
    private final LoginGuard guard;

    public AuditFilter(AuditService audit, LoginGuard guard) {
        this.audit = audit;
        this.guard = guard;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(req, res);
        } finally {
            try { log(req, res); } catch (Exception ignored) { }
        }
    }

    private void log(HttpServletRequest req, HttpServletResponse res) {
        String path = req.getRequestURI();
        if (path == null || !path.startsWith("/api/") || path.startsWith("/api/auth/")) return;   // sign-in events are logged by AuthController
        int status = res.getStatus();
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        User me = a != null && a.getPrincipal() instanceof User u ? u : null;
        boolean staff = me != null && (me.getRole() == Role.ADMIN || me.getRole() == Role.COUNTY_ADMIN || me.getRole() == Role.OFFICER);
        boolean write = !READ.contains(req.getMethod());
        String ip = ClientIp.of(req);
        String what = req.getMethod() + " " + path;

        if (status == 403 || (status == 401 && (path.startsWith("/api/admin") || path.startsWith("/api/payments/admin")))) {
            try { guard.limit("audit-denied", ip, 30, Duration.ofMinutes(1)); } catch (Exception tooMany) { return; }   // do not let a flood fill the log
            audit.record(me, "ACCESS_DENIED", what, "HTTP " + status, ip, false);
            return;
        }
        if (write && staff && status < 400) audit.record(me, "STAFF_ACTION", what, "HTTP " + status, ip, true);
        else if (write && staff) audit.record(me, "STAFF_ACTION_FAILED", what, "HTTP " + status, ip, false);
    }
}

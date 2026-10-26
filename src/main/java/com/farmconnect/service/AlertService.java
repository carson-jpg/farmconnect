package com.farmconnect.service;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.AuditLogRepository;
import com.farmconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Emails the super admin when something needs attention: failed backup, low disk, database down, a burst of failed logins. Each alert at most every 6 hours. */
@Service
public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final long WINDOW = Duration.ofHours(6).toMillis();

    private final UserRepository users;
    private final MailService mail;
    private final AuditLogRepository audit;
    private final JdbcTemplate jdbc;
    private final BackupService backups;
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    @Value("${app.upload-dir:./uploads}") private String uploadDir;
    @Value("${app.alerts.enabled:true}") private boolean enabled;
    @Value("${app.alerts.failed-logins:200}") private int failedLoginThreshold;
    @Value("${app.backup.enabled:false}") private boolean backupEnabled;

    public AlertService(UserRepository users, MailService mail, AuditLogRepository audit, JdbcTemplate jdbc, BackupService backups) {
        this.users = users;
        this.mail = mail;
        this.audit = audit;
        this.jdbc = jdbc;
        this.backups = backups;
    }

    /** Log it always; email the super admins unless the same alert went out recently. */
    public void alert(String key, String subject, String body) {
        log.error("ALERT {}: {}", key, body);
        if (!enabled || !BackupSupport.shouldSend(lastSent, key, System.currentTimeMillis(), WINDOW)) return;
        try {
            List<User> admins = users.findByRoleIn(List.of(Role.ADMIN));
            for (User u : admins) if (u.isEnabled() && u.getEmail() != null) mail.send(u.getEmail(), "[FarmConnect] " + subject, body);
        } catch (Exception e) {
            log.warn("Could not send alert email: {}", e.getMessage());
        }
    }

    /** Every 10 minutes. */
    @Scheduled(initialDelay = 120_000, fixedDelay = 600_000)
    public void watch() {
        try { jdbc.queryForObject("select 1", Integer.class); }
        catch (Exception e) { alert("db-down", "The database is not answering", "The health check query failed: " + e.getMessage()); return; }

        File dir = new File(uploadDir).getAbsoluteFile();
        File probe = dir.exists() ? dir : dir.getParentFile();
        if (probe != null && BackupSupport.diskLow(probe.getUsableSpace(), probe.getTotalSpace()))
            alert("disk-low", "The server disk is almost full",
                    "Free space is " + BackupSupport.humanSize(probe.getUsableSpace()) + " of " + BackupSupport.humanSize(probe.getTotalSpace())
                            + ". Free some space or enlarge the disk before backups and uploads start to fail.");

        long failed = audit.countByActionAndAtAfter("LOGIN_FAILED", Instant.now().minus(Duration.ofMinutes(10)));
        if (BackupSupport.loginSpike(failed, failedLoginThreshold))
            alert("login-spike", "Many failed logins", failed + " failed logins in the last 10 minutes. Someone may be guessing passwords. "
                    + "Open Security in the super admin console to see where they come from and who is locked out.");

        if (backupEnabled) {
            Instant last = backups.lastSuccess();
            if (last == null || last.isBefore(Instant.now().minus(Duration.ofHours(36))))
                alert("backup-stale", "No recent database backup", "The last successful backup was "
                        + (last == null ? "never (since this server started)" : last.toString())
                        + ". Open System health in the super admin console and run a backup, then check the backup folder and pg_dump.");
        }
    }
}

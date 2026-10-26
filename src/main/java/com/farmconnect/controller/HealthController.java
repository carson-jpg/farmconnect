package com.farmconnect.controller;

import com.farmconnect.service.BackupService;
import com.farmconnect.service.BackupSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** /api/health is for uptime checkers (no secrets). The /api/admin/... endpoints are for the super admin's System health page. */
@RestController
@RequiredArgsConstructor
public class HealthController {
    private final JdbcTemplate jdbc;
    private final BackupService backups;

    @Value("${app.upload-dir:./uploads}") private String uploadDir;
    @Value("${spring.profiles.active:default}") private String profile;

    /** 200 when the app and database answer, 503 otherwise. */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean up = dbMillis() >= 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", up ? "UP" : "DOWN");
        m.put("time", Instant.now().toString());
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(m);
    }

    @GetMapping("/api/admin/health")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> details() {
        Map<String, Object> m = new LinkedHashMap<>();
        long db = dbMillis();
        m.put("database", db >= 0 ? "UP" : "DOWN");
        m.put("databaseMillis", db);
        File disk = new File(uploadDir).getAbsoluteFile();
        File probe = disk.exists() ? disk : disk.getParentFile();
        long free = probe == null ? 0 : probe.getUsableSpace(), total = probe == null ? 0 : probe.getTotalSpace();
        m.put("diskFreeBytes", free);
        m.put("diskTotalBytes", total);
        m.put("diskLow", BackupSupport.diskLow(free, total));
        Runtime rt = Runtime.getRuntime();
        m.put("memoryUsedBytes", rt.totalMemory() - rt.freeMemory());
        m.put("memoryMaxBytes", rt.maxMemory());
        m.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        m.put("profile", profile);
        m.put("backupsEnabled", backups.isEnabled());
        m.put("backupRunning", backups.isRunning());
        m.put("backupFolder", backups.directory());
        BackupService.Result r = backups.last();
        m.put("lastBackupOk", r == null ? null : r.ok());
        m.put("lastBackupMessage", r == null ? null : r.message());
        m.put("lastBackupAt", r == null ? null : r.at().toString());
        m.put("lastSuccessfulBackupAt", backups.lastSuccess() == null ? null : backups.lastSuccess().toString());
        return m;
    }

    @GetMapping("/api/admin/backups")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BackupService.FileInfo> files() { return backups.list(); }

    /** Starts a backup in the background (it can take minutes); the System health page shows the result. */
    @PostMapping("/api/admin/backups/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> runNow() {
        if (backups.isRunning()) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "A backup is already running"));
        CompletableFuture.runAsync(() -> backups.run("started by the super admin"));
        return ResponseEntity.accepted().body(Map.of("message", "Backup started"));
    }

    private long dbMillis() {
        long t = System.nanoTime();
        try { jdbc.queryForObject("select 1", Integer.class); return (System.nanoTime() - t) / 1_000_000; }
        catch (Exception e) { return -1; }
    }
}

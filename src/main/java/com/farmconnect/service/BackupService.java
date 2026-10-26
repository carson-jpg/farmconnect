package com.farmconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Backs up the database (PostgreSQL with pg_dump, or the H2 development file) and the uploaded photos and documents,
 * checks that the database backup can be read, keeps the newest copies and optionally copies them to a second folder.
 * Needs the PostgreSQL client tools (pg_dump, pg_restore) on the server when PostgreSQL is used.
 */
@Service
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    public record Result(boolean ok, String message, Instant at, List<String> files) {}
    public record FileInfo(String name, long bytes, Instant modified) {}

    private final JdbcTemplate jdbc;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Result last;
    private volatile Instant lastSuccess;

    @Value("${spring.datasource.url}") private String jdbcUrl;
    @Value("${spring.datasource.username:}") private String dbUser;
    @Value("${spring.datasource.password:}") private String dbPassword;
    @Value("${app.upload-dir:./uploads}") private String uploadDir;
    @Value("${app.backup.enabled:false}") private boolean enabled;
    @Value("${app.backup.dir:./backups}") private String backupDir;
    @Value("${app.backup.copy-to:}") private String copyTo;
    @Value("${app.backup.keep:14}") private int keep;

    private AlertService alerts;

    public BackupService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Set after construction: AlertService needs this service too. */
    @org.springframework.beans.factory.annotation.Autowired
    public void setAlerts(@org.springframework.context.annotation.Lazy AlertService alerts) { this.alerts = alerts; }

    public Result last() { return last; }
    public Instant lastSuccess() { return lastSuccess; }
    public boolean isRunning() { return running.get(); }
    public boolean isEnabled() { return enabled; }
    public String directory() { return Paths.get(backupDir).toAbsolutePath().normalize().toString(); }

    @Scheduled(cron = "${app.backup.cron:0 30 2 * * *}")
    public void nightly() {
        if (enabled) run("scheduled");
    }

    public List<FileInfo> list() {
        File[] files = new File(directory()).listFiles(f -> f.isFile());
        List<FileInfo> out = new ArrayList<>();
        if (files != null) for (File f : files) out.add(new FileInfo(f.getName(), f.length(), Instant.ofEpochMilli(f.lastModified())));
        out.sort(Comparator.comparing(FileInfo::modified).reversed());
        return out;
    }

    /** One backup at a time. Returns the outcome; failures also raise an alert. */
    public Result run(String reason) {
        if (!running.compareAndSet(false, true)) return new Result(false, "A backup is already running", Instant.now(), List.of());
        List<String> made = new ArrayList<>();
        try {
            Path dir = Paths.get(directory());
            Files.createDirectories(dir);
            LocalDateTime now = LocalDateTime.now();
            made.add(backupDatabase(dir, now));
            String zip = backupUploads(dir, now);
            if (zip != null) made.add(zip);
            for (String old : BackupSupport.expired(namesIn(dir), keep)) Files.deleteIfExists(dir.resolve(old));
            copyOffsite(dir, made);
            Result r = new Result(true, "Backup finished (" + reason + "): " + String.join(", ", made), Instant.now(), made);
            last = r;
            lastSuccess = r.at();
            log.info(r.message());
            return r;
        } catch (Exception e) {
            Result r = new Result(false, "Backup FAILED (" + reason + "): " + e.getMessage(), Instant.now(), made);
            last = r;
            log.error(r.message(), e);
            if (alerts != null) alerts.alert("backup-failed", "The database backup failed", r.message());
            return r;
        } finally {
            running.set(false);
        }
    }

    private static List<String> namesIn(Path dir) throws IOException {
        try (var s = Files.list(dir)) { return s.map(p -> p.getFileName().toString()).toList(); }
    }

    private String backupDatabase(Path dir, LocalDateTime now) throws Exception {
        BackupSupport.PgTarget pg = BackupSupport.parsePgUrl(jdbcUrl);
        if (pg != null) {
            String name = BackupSupport.fileName("farmconnect-db", now, "dump");
            Path out = dir.resolve(name);
            List<String> cmd = List.of("pg_dump", "-Fc", "-h", pg.host(), "-p", String.valueOf(pg.port()), "-U", dbUser, "-f", out.toString(), pg.database());
            exec(cmd, 30, true);
            if (Files.size(out) < 100) throw new IOException("the backup file is empty");
            exec(List.of("pg_restore", "--list", out.toString()), 5, false);          // proves the file is a readable archive
            return name;
        }
        if (BackupSupport.isH2(jdbcUrl)) {
            String name = BackupSupport.fileName("farmconnect-db", now, "sql");
            Path out = dir.resolve(name).toAbsolutePath();
            jdbc.execute("SCRIPT TO '" + out.toString().replace("'", "''") + "'");
            if (!Files.exists(out) || Files.size(out) < 100) throw new IOException("the backup file is empty");
            return name;
        }
        throw new IOException("backups support PostgreSQL and H2 only");
    }

    private void exec(List<String> cmd, int minutes, boolean withPassword) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        if (withPassword && dbPassword != null && !dbPassword.isEmpty()) pb.environment().put("PGPASSWORD", dbPassword);
        Process p = pb.start();
        String output;
        try (InputStream in = p.getInputStream()) { output = new String(in.readNBytes(4000)); }
        if (!p.waitFor(minutes, TimeUnit.MINUTES)) { p.destroyForcibly(); throw new IOException(cmd.get(0) + " took too long"); }
        if (p.exitValue() != 0) throw new IOException(cmd.get(0) + " failed: " + output.trim());
    }

    /** All uploaded photos and documents, as one zip. Skipped when there are none. */
    private String backupUploads(Path dir, LocalDateTime now) throws IOException {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) return null;
        String name = BackupSupport.fileName("farmconnect-uploads", now, "zip");
        int count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(dir.resolve(name))));
             var walk = Files.walk(root)) {
            for (Path p : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                zip.putNextEntry(new ZipEntry(root.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, zip);
                zip.closeEntry();
                count++;
            }
        }
        if (count == 0) { Files.deleteIfExists(dir.resolve(name)); return null; }
        return name;
    }

    private void copyOffsite(Path dir, List<String> made) throws IOException {
        if (copyTo == null || copyTo.isBlank()) return;
        Path target = Paths.get(copyTo).toAbsolutePath().normalize();
        Files.createDirectories(target);
        for (String n : made) Files.copy(dir.resolve(n), target.resolve(n), StandardCopyOption.REPLACE_EXISTING);
        for (String old : BackupSupport.expired(namesIn(target), keep)) Files.deleteIfExists(target.resolve(old));
    }
}

package com.farmconnect.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The small decisions behind backups and alerts, kept free of Spring and the file system so they are easy to test. */
public final class BackupSupport {
    private BackupSupport() {}

    public record PgTarget(String host, int port, String database) {}

    private static final Pattern PG = Pattern.compile("^jdbc:postgresql://([^/:?]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?$");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final Pattern STAMPED = Pattern.compile("^(.*)-(\\d{8}-\\d{4})(\\.[A-Za-z0-9]+)$");

    /** Host, port and database from a PostgreSQL JDBC url, or null when the url is for another database. */
    public static PgTarget parsePgUrl(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        Matcher m = PG.matcher(jdbcUrl.trim());
        if (!m.matches()) return null;
        return new PgTarget(m.group(1), m.group(2) == null ? 5432 : Integer.parseInt(m.group(2)), m.group(3));
    }

    public static boolean isH2(String jdbcUrl) { return jdbcUrl != null && jdbcUrl.trim().startsWith("jdbc:h2:"); }

    public static String stamp(LocalDateTime t) { return t.format(STAMP); }

    public static String fileName(String prefix, LocalDateTime t, String extension) { return prefix + "-" + stamp(t) + "." + extension; }

    /** Which backup files to delete so only the newest {@code keep} of each kind remain. Files that are not ours are never listed. */
    public static List<String> expired(List<String> names, int keep) {
        Map<String, List<String>> byKind = new java.util.TreeMap<>();
        for (String n : names) {
            Matcher m = STAMPED.matcher(n);
            if (m.matches()) byKind.computeIfAbsent(m.group(1) + m.group(3), k -> new ArrayList<>()).add(n);
        }
        List<String> out = new ArrayList<>();
        for (List<String> group : byKind.values()) {
            group.sort(Comparator.comparing((String n) -> { Matcher m = STAMPED.matcher(n); m.matches(); return m.group(2); }).reversed());
            for (int i = Math.max(1, keep); i < group.size(); i++) out.add(group.get(i));
        }
        return out;
    }

    /** Less than 1 GB free, or less than 10% of the disk. */
    public static boolean diskLow(long freeBytes, long totalBytes) {
        if (totalBytes <= 0) return false;
        return freeBytes < 1024L * 1024 * 1024 || freeBytes * 100 / totalBytes < 10;
    }

    public static boolean loginSpike(long failedInWindow, int threshold) { return threshold > 0 && failedInWindow >= threshold; }

    /** True (and remembers the time) when this alert has not been sent within the window. */
    public static boolean shouldSend(Map<String, Long> lastSent, String key, long nowMillis, long windowMillis) {
        Long last = lastSent.get(key);
        if (last != null && nowMillis - last < windowMillis) return false;
        lastSent.put(key, nowMillis);
        return true;
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double v = bytes / 1024.0;
        String[] u = {"KB", "MB", "GB", "TB"};
        int i = 0;
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(java.util.Locale.US, "%.1f %s", v, u[i]);
    }
}

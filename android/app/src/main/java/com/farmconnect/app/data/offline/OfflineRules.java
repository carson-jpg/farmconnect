package com.farmconnect.app.data.offline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What the phone may keep for offline use and which changes it may hold until the network is back. No Android code, so it is easy to test. */
public final class OfflineRules {
    private OfflineRules() {}

    private static final String[] NEVER_SAVE = {"/auth", "/payments", "/security", "/files/", "/ussd", "/sms", "/otp", "/password", "/pay/"};
    private static final Pattern RECORD_ID = Pattern.compile("^/api/records/(-?\\d+)$");

    /** Information the phone saves after every successful read, to show again when there is no network. */
    public static boolean cacheable(String path) {
        if (path == null || !path.startsWith("/api/")) return false;
        String p = path.toLowerCase(java.util.Locale.ROOT);
        for (String bad : NEVER_SAVE) if (p.contains(bad)) return false;
        return true;
    }

    /** Only farm records can be changed without a network (add, edit, delete). Everything else needs the server. */
    public static boolean queueable(String method, String path) {
        if (method == null || path == null) return false;
        if ("POST".equals(method)) return "/api/records".equals(path);
        if ("PUT".equals(method) || "DELETE".equals(method)) return RECORD_ID.matcher(path).matches();
        return false;
    }

    /** The id in /api/records/{id}, or null. Records created offline get a negative id until they are sent. */
    public static Long recordId(String path) {
        if (path == null) return null;
        Matcher m = RECORD_ID.matcher(path);
        return m.matches() ? Long.valueOf(m.group(1)) : null;
    }

    public static boolean isTempId(Long id) { return id != null && id < 0; }

    public static String rewriteId(String path, long from, long to) {
        return path.equals("/api/records/" + from) ? "/api/records/" + to : path;
    }

    /** "type=HARVEST&x=1" -> "HARVEST"; null when absent or blank. */
    public static String queryParam(String query, String name) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).equals(name)) {
                String v = part.substring(eq + 1);
                return v.isEmpty() ? null : v;
            }
        }
        return null;
    }
}

package com.farmconnect.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stops password guessing. Three counters, each with a 15 minute lock once it is exceeded:
 *  - one account from one address (5 wrong passwords),
 *  - one account from anywhere (20 wrong passwords, so many addresses cannot split the guesses),
 *  - one address against any account (200 failures).
 * A successful sign-in clears the account counters. The super admin can clear a lock by hand. Kept in memory,
 * so a restart also clears them (the audit log keeps the history).
 */
@Service
public class LoginGuard {
    static final int PER_PAIR = 5, PER_ACCOUNT = 20, PER_IP = 200;
    static final Duration WINDOW = Duration.ofMinutes(15), LOCK = Duration.ofMinutes(15);

    private static final class Entry { int fails; Instant first = Instant.now(); Instant lockedUntil; }
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, Hits> rate = new ConcurrentHashMap<>();
    private static final class Hits { final List<Instant> hits = new ArrayList<>(); }

    private static String acct(String email) { return "a|" + norm(email); }
    private static String pair(String email, String ip) { return "p|" + norm(email) + "|" + ip; }
    private static String addr(String ip) { return "i|" + ip; }
    private static String norm(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }

    /** Throws 429 when this sign-in attempt is locked out. Call BEFORE checking the password. */
    public void check(String email, String ip) {
        Instant now = Instant.now();
        for (String k : new String[]{pair(email, ip), acct(email), addr(ip)}) {
            Entry e = entries.get(k);
            if (e != null && e.lockedUntil != null && e.lockedUntil.isAfter(now)) {
                long mins = Math.max(1, Duration.between(now, e.lockedUntil).toMinutes() + 1);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed attempts. Try again in " + mins + " minute" + (mins == 1 ? "" : "s") + ".");
            }
        }
    }

    /** @return true when this failure caused a lock. */
    public boolean fail(String email, String ip) {
        boolean locked = hit(pair(email, ip), PER_PAIR);
        locked |= hit(acct(email), PER_ACCOUNT);
        locked |= hit(addr(ip), PER_IP);
        return locked;
    }

    public void success(String email, String ip) {
        entries.remove(pair(email, ip));
        entries.remove(acct(email));
    }

    private boolean hit(String key, int max) {
        Instant now = Instant.now();
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        synchronized (e) {
            if (e.lockedUntil != null && e.lockedUntil.isAfter(now)) return false;
            if (e.first.isBefore(now.minus(WINDOW)) || e.lockedUntil != null) { e.fails = 0; e.first = now; e.lockedUntil = null; }
            e.fails++;
            if (e.fails >= max) { e.lockedUntil = now.plus(LOCK); return true; }
            return false;
        }
    }

    /** Generic limiter for other endpoints: at most {@code max} calls per key per window; throws 429 beyond that. */
    public void limit(String bucket, String key, int max, Duration window) {
        Instant now = Instant.now();
        Hits d = rate.computeIfAbsent(bucket + "|" + key, k -> new Hits());
        synchronized (d) {
            d.hits.removeIf(t -> t.isBefore(now.minus(window)));
            if (d.hits.size() >= max) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a few minutes and try again.");
            d.hits.add(now);
        }
        if (rate.size() > 20_000) rate.entrySet().removeIf(en -> en.getValue().hits.isEmpty());
    }

    /** A lock that is currently active, for the super admin's screen. */
    public record Lock(String kind, String subject, Instant until) {}

    public List<Lock> activeLocks() {
        Instant now = Instant.now();
        List<Lock> out = new ArrayList<>();
        entries.forEach((k, e) -> {
            if (e.lockedUntil == null || !e.lockedUntil.isAfter(now)) return;
            String[] p = k.split("\\|", 3);
            switch (p[0]) {
                case "a" -> out.add(new Lock("ACCOUNT", p[1], e.lockedUntil));
                case "p" -> out.add(new Lock("ACCOUNT_FROM_ADDRESS", p[1] + " from " + p[2], e.lockedUntil));
                default -> out.add(new Lock("ADDRESS", p[1], e.lockedUntil));
            }
        });
        out.sort((x, y) -> y.until().compareTo(x.until()));
        return out;
    }

    /** Clear every lock and counter for an account (all addresses). @return how many were cleared. */
    public int unlockAccount(String email) {
        String n = norm(email);
        int[] c = {0};
        entries.keySet().removeIf(k -> {
            boolean hit = k.equals("a|" + n) || k.startsWith("p|" + n + "|");
            if (hit) c[0]++;
            return hit;
        });
        return c[0];
    }

    public int unlockAddress(String ip) { return entries.remove(addr(ip)) == null ? 0 : 1; }
}

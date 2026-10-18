package com.farmconnect.service;

import com.farmconnect.model.AuditLog;
import com.farmconnect.model.User;
import com.farmconnect.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/** Writes the security trail. Never throws: a logging problem must not break the action being logged. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repo;
    /** Events are written by two background threads so a login never waits for the log. If a huge burst fills the queue, extra events are dropped rather than slowing the app. */
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(20_000), r -> { Thread t = new Thread(r, "audit-writer"); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.DiscardPolicy());

    @PreDestroy
    void stop() { writer.shutdown(); }

    public void record(User actor, String action, String target, String detail, String ip, boolean success) {
        record(actor == null ? null : actor.getId(), actor == null ? null : actor.getEmail(),
                actor == null || actor.getRole() == null ? null : actor.getRole().name(), action, target, detail, ip, success);
    }

    public void record(Long actorId, String actorEmail, String actorRole, String action, String target, String detail, String ip, boolean success) {
        final Instant at = Instant.now();
        try {
            writer.execute(() -> write(at, actorId, actorEmail, actorRole, action, target, detail, ip, success));
        } catch (Exception ignored) { /* never break the request */ }
    }

    private void write(Instant at, Long actorId, String actorEmail, String actorRole, String action, String target, String detail, String ip, boolean success) {
        try {
            AuditLog a = new AuditLog();
            a.setAt(at);
            a.setActorId(actorId);
            a.setActorEmail(cut(actorEmail, 200));
            a.setActorRole(actorRole);
            a.setAction(cut(action, 40));
            a.setTarget(cut(target, 300));
            a.setDetail(cut(detail, 500));
            a.setIp(cut(ip, 64));
            a.setSuccess(success);
            repo.save(a);
        } catch (Exception ignored) { /* never break the request */ }
    }

    /** Keep six months of history. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOld() {
        try { repo.deleteByAtBefore(Instant.now().minus(Duration.ofDays(180))); } catch (Exception ignored) { }
    }

    private static String cut(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }
}

package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.AppNotification;
import com.farmconnect.repository.AppNotificationRepository;
import com.farmconnect.repository.ChatMessageRepository;
import com.farmconnect.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final AppNotificationRepository repo;
    private final ChatMessageRepository messages;

    @GetMapping
    @Transactional(readOnly = true)
    public List<NotificationResponse> mine(@AuthenticationPrincipal User u) {
        return repo.findTop100ByUserIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getRefId(),
                        n.getCreatedAt(), n.getReadAt() != null)).toList();
    }

    /** Unread bell notifications + unread chat messages, for the badges on the dashboards. */
    @GetMapping("/counts")
    public Counts counts(@AuthenticationPrincipal User u) {
        return new Counts(repo.countByUserIdAndReadAtIsNull(u.getId()), messages.countByRecipientIdAndReadAtIsNull(u.getId()));
    }

    @PostMapping("/read-all")
    @Transactional
    public Counts readAll(@AuthenticationPrincipal User u) {
        Instant now = Instant.now();
        List<AppNotification> unread = repo.findByUserIdAndReadAtIsNull(u.getId());
        unread.forEach(n -> n.setReadAt(now));
        repo.saveAll(unread);
        return counts(u);
    }

    @PostMapping("/{id}/read")
    @Transactional
    public Counts read(@PathVariable Long id, @AuthenticationPrincipal User u) {
        AppNotification n = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (!n.getUser().getId().equals(u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not yours");
        if (n.getReadAt() == null) { n.setReadAt(Instant.now()); repo.save(n); }
        return counts(u);
    }
}

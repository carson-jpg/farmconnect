package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/** One-to-one messaging between farmers, buyers and county staff. */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final OrderRepository orders;
    private final NotificationService notifier;

    @GetMapping("/conversations")
    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations(@AuthenticationPrincipal User me) {
        Map<Long, ChatMessage> last = new LinkedHashMap<>();
        Map<Long, Long> unread = new HashMap<>();
        for (ChatMessage m : messages.involving(me.getId())) {   // newest first
            User other = m.getSender().getId().equals(me.getId()) ? m.getRecipient() : m.getSender();
            last.putIfAbsent(other.getId(), m);
            if (m.getRecipient().getId().equals(me.getId()) && m.getReadAt() == null) unread.merge(other.getId(), 1L, Long::sum);
        }
        List<ConversationResponse> out = new ArrayList<>();
        for (var e : last.entrySet()) {
            ChatMessage m = e.getValue();
            User other = m.getSender().getId().equals(me.getId()) ? m.getRecipient() : m.getSender();
            out.add(new ConversationResponse(other.getId(), other.getName(), other.getRole(), other.isVerified(),
                    m.getBody(), m.getCreatedAt(), unread.getOrDefault(other.getId(), 0L)));
        }
        return out;
    }

    @GetMapping("/with/{userId}")
    @Transactional
    public List<ChatMessageResponse> thread(@PathVariable Long userId, @AuthenticationPrincipal User me) {
        List<ChatMessage> list = messages.between(me.getId(), userId);
        Instant now = Instant.now();
        for (ChatMessage m : list) {
            if (m.getRecipient().getId().equals(me.getId()) && m.getReadAt() == null) { m.setReadAt(now); messages.save(m); }
        }
        return list.stream().map(m -> new ChatMessageResponse(m.getId(), m.getSender().getId(), m.getRecipient().getId(),
                m.getBody(), m.getCreatedAt(), m.getSender().getId().equals(me.getId()))).toList();
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@Valid @RequestBody ChatRequest r, @AuthenticationPrincipal User me) {
        User to = users.findById(r.recipientId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (to.getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot message yourself");
        if (!to.isEnabled()) throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is not available");
        if (!allowed(me, to)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot message this user");
        ChatMessage m = new ChatMessage();
        m.setSender(me);
        m.setRecipient(to);
        m.setBody(r.body().trim());
        m = messages.save(m);
        notifier.notify(to, "MESSAGE", "New message from " + me.getName(), r.body().trim(), me.getId());
        return new ChatMessageResponse(m.getId(), me.getId(), to.getId(), m.getBody(), m.getCreatedAt(), true);
    }

    /** People I can start a conversation with. */
    @GetMapping("/contacts")
    @Transactional(readOnly = true)
    public List<ContactResponse> contacts(@RequestParam(required = false) String q, @AuthenticationPrincipal User me) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return users.findAll().stream()
                .filter(u -> !u.getId().equals(me.getId()) && u.isEnabled())
                .filter(u -> allowed(me, u))
                .filter(u -> needle.isEmpty() || (u.getName() != null && u.getName().toLowerCase().contains(needle)))
                .sorted(Comparator.comparing((User u) -> u.getRole().ordinal()).thenComparing(u -> String.valueOf(u.getName())))
                .limit(200)
                .map(u -> new ContactResponse(u.getId(), u.getName(), u.getRole(), u.isVerified())).toList();
    }

    private boolean staff(User u) { return u.getRole() == Role.ADMIN || u.getRole() == Role.OFFICER; }

    /** Staff can talk to anyone; farmers (verified) and buyers can talk to each other; buyers cannot message buyers. */
    private boolean allowed(User a, User b) {
        if (staff(a) || staff(b)) return true;
        if (a.getRole() == Role.BUYER && b.getRole() == Role.BUYER) return false;
        User farmerSide = a.getRole() == Role.FARMER ? a : b;
        User other = farmerSide == a ? b : a;
        // an unverified farmer is not public: only people they already traded with can reach them
        if (!farmerSide.isVerified() && other.getRole() == Role.BUYER)
            return orders.findForFarmer(farmerSide.getId()).stream().anyMatch(o -> o.getBuyer().getId().equals(other.getId()));
        return true;
    }
}

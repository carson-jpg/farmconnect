package com.farmconnect.service;

import com.farmconnect.model.AppNotification;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.AppNotificationRepository;
import com.farmconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** In-app notifications (the bell) plus optional SMS through SmsService (Africa's Talking when configured). */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppNotificationRepository repo;
    private final UserRepository users;
    private final SmsService sms;

    /** app.sms.notify=true also texts users for important events (new order, order status, verification). */
    @Value("${app.sms.notify:false}") private boolean smsNotify;

    @Transactional
    public void notify(User to, String type, String title, String body, Long refId) {
        if (to == null) return;
        AppNotification n = new AppNotification();
        n.setUser(to);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body == null ? null : body.length() > 500 ? body.substring(0, 497) + "..." : body);
        n.setRefId(refId);
        repo.save(n);
    }

    /** In-app notification and, when enabled, an SMS to the user's phone. */
    @Transactional
    public void notifyAndSms(User to, String type, String title, String body, Long refId) {
        notify(to, type, title, body, refId);
        if (smsNotify) text(to, title + ". " + body);
    }

    /** Notify everybody in an audience: ALL, FARMERS or BUYERS. Staff are always included. Returns how many. */
    @Transactional
    public int broadcast(String audience, String type, String title, String body, Long refId, boolean alsoSms) {
        String a = audience == null ? "ALL" : audience.toUpperCase();
        List<User> targets = users.findAll().stream().filter(User::isEnabled).filter(u -> switch (a) {
            case "FARMERS" -> u.getRole() == Role.FARMER || u.getRole() == Role.ADMIN || u.getRole() == Role.OFFICER;
            case "BUYERS" -> u.getRole() == Role.BUYER || u.getRole() == Role.ADMIN || u.getRole() == Role.OFFICER;
            default -> true;
        }).toList();
        for (User u : targets) {
            notify(u, type, title, body, refId);
            if (alsoSms && u.getRole() != Role.ADMIN) text(u, title + ". " + body);
        }
        return targets.size();
    }

    private void text(User u, String message) {
        if (u.getPhone() == null || u.getPhone().isBlank()) return;
        try {
            sms.send(toE164(u.getPhone()), message.length() > 300 ? message.substring(0, 297) + "..." : message);
        } catch (Exception e) {
            log.warn("SMS to user {} failed: {}", u.getId(), e.getMessage());
        }
    }

    static String toE164(String phone) {
        String p = phone.replaceAll("[\\s-]", "");
        if (p.startsWith("+")) return p;
        if (p.startsWith("254")) return "+" + p;
        if (p.startsWith("0")) return "+254" + p.substring(1);
        return p;
    }
}

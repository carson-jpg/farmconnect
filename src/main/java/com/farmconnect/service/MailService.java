package com.farmconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sends plain-text email. When spring.mail.host is NOT set there is no mail sender, so the message is printed
 * in the server console instead (development) - same idea as SmsService with app.sms.provider=log.
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    @Value("${app.mail.from:}") private String from;

    public MailService(ObjectProvider<JavaMailSender> sender) { this.sender = sender; }

    public record Attachment(String filename, String contentType, byte[] data) {}

    /** Plain-text email with files attached (the weekly county reports). Printed to the console when no mail server is set. */
    public void sendWithAttachments(String to, String subject, String text, java.util.List<Attachment> files) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.warn("[MAIL-DEV] to {} | {} | {} | attachments: {}", to, subject, text,
                    files.stream().map(f -> f.filename() + " (" + f.data().length + " bytes)").toList());
            return;
        }
        try {
            var m = s.createMimeMessage();
            var h = new MimeMessageHelper(m, true, "UTF-8");
            if (from != null && !from.isBlank()) h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(text, false);
            for (Attachment a : files) h.addAttachment(a.filename(), new ByteArrayResource(a.data()), a.contentType());
            s.send(m);
        } catch (Exception e) {
            log.error("Email with attachments failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send email");
        }
    }

    public void send(String to, String subject, String text) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.warn("[MAIL-DEV] to {} | {} | {}", to, subject, text);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            if (from != null && !from.isBlank()) m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            s.send(m);
        } catch (Exception e) {
            log.error("Email failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send email");
        }
    }
}

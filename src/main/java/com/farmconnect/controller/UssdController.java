package com.farmconnect.controller;

import com.farmconnect.service.AssistantService;
import com.farmconnect.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Africa's Talking callbacks for feature phones (no login: protected by a shared secret in the URL).
 *
 *   USSD:  POST https://YOUR-SERVER/api/ussd?key=SECRET          (form: sessionId, serviceCode, phoneNumber, text)
 *   SMS:   POST https://YOUR-SERVER/api/sms/inbound?key=SECRET   (form: from, to, text, date, id)
 *
 * Set app.ussd.secret in application.properties. If it is empty the endpoints still work but log a warning (dev only).
 */
@RestController
@RequiredArgsConstructor
public class UssdController {
    private static final Logger log = LoggerFactory.getLogger(UssdController.class);

    private final AssistantService assistant;
    private final SmsService sms;

    @Value("${app.ussd.secret:}") private String secret;

    @PostMapping(value = "/api/ussd", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ussd(@RequestParam(required = false) String key,
                                       @RequestParam(required = false, defaultValue = "") String phoneNumber,
                                       @RequestParam(required = false, defaultValue = "") String text) {
        if (!allowed(key)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("END Not allowed");
        String reply;
        try {
            reply = assistant.ussd(phoneNumber, text);
        } catch (Exception e) {
            log.warn("USSD error: {}", e.toString());
            reply = "END Samahani, hitilafu imetokea. Jaribu tena. / Sorry, something went wrong.";
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).body(reply);
    }

    @PostMapping("/api/sms/inbound")
    public ResponseEntity<String> inbound(@RequestParam(required = false) String key,
                                          @RequestParam(required = false, defaultValue = "") String from,
                                          @RequestParam(required = false, defaultValue = "") String text) {
        if (!allowed(key)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not allowed");
        if (from.isBlank()) return ResponseEntity.badRequest().body("Missing sender");
        try {
            String reply = assistant.sms(from, text);
            if (reply != null && !reply.isBlank()) sms.send(from.startsWith("+") ? from : "+" + from, reply);
        } catch (Exception e) {
            log.warn("Inbound SMS error: {}", e.toString());
        }
        return ResponseEntity.ok("OK");
    }

    private boolean allowed(String key) {
        if (secret == null || secret.isBlank()) { log.warn("app.ussd.secret is not set: USSD/SMS callbacks are open (development only)"); return true; }
        return secret.equals(key);
    }
}

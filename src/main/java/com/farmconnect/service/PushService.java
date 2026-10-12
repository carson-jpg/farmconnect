package com.farmconnect.service;

import com.farmconnect.model.DeviceToken;
import com.farmconnect.repository.DeviceTokenRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends a push notification to every phone a user has signed in on, through Firebase Cloud Messaging.
 * Switched off (and harmless) until Firebase credentials are configured:
 *   FIREBASE_CREDENTIALS_FILE = path to the service-account .json, or
 *   FIREBASE_CREDENTIALS_JSON = the whole .json text (handy on Render and other hosts).
 * Messages carry data only; the app builds the notification itself so it can pick the language and the screen to open.
 */
@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final String APP_NAME = "farmconnect";

    private final DeviceTokenRepository tokens;
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private FirebaseMessaging fcm;

    @Value("${app.fcm.credentials-file:}") private String credentialsFile;
    @Value("${app.fcm.credentials-json:}") private String credentialsJson;

    public PushService(DeviceTokenRepository tokens) { this.tokens = tokens; }

    @PostConstruct
    void init() {
        try {
            InputStream in = open();
            if (in == null) { log.info("Push notifications are off: no Firebase credentials configured"); return; }
            try (InputStream s = in) {
                FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(s)).build();
                FirebaseApp app = FirebaseApp.getApps().stream().filter(a -> APP_NAME.equals(a.getName())).findFirst()
                        .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
                fcm = FirebaseMessaging.getInstance(app);
                log.info("Push notifications are on");
            }
        } catch (Exception e) {
            log.error("Firebase could not start, push notifications stay off: {}", e.getMessage());
        }
    }

    private InputStream open() throws IOException {
        if (credentialsJson != null && !credentialsJson.isBlank())
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        if (credentialsFile != null && !credentialsFile.isBlank()) return new FileInputStream(credentialsFile);
        return null;
    }

    @PreDestroy
    void stop() { pool.shutdown(); }

    public boolean enabled() { return fcm != null; }

    /** Queue a push. Waits for the database transaction to commit, so nobody is told about something that rolled back. */
    public void send(Long userId, Long notificationId, String type, String title, String body, Long refId) {
        if (fcm == null || userId == null) return;
        Runnable queue = () -> pool.submit(() -> deliver(userId, notificationId, type, title, body, refId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { queue.run(); }
            });
        } else {
            queue.run();
        }
    }

    private void deliver(Long userId, Long notificationId, String type, String title, String body, Long refId) {
        try {
            for (DeviceToken d : tokens.findByUserId(userId)) {
                Message message = Message.builder()
                        .setToken(d.getToken())
                        .putData("title", nz(title))
                        .putData("body", nz(body))
                        .putData("type", nz(type))
                        .putData("refId", refId == null ? "" : String.valueOf(refId))
                        .putData("notificationId", notificationId == null ? "" : String.valueOf(notificationId))
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setTtl(24L * 3600 * 1000)
                                .build())
                        .build();
                try {
                    fcm.send(message);
                } catch (FirebaseMessagingException e) {
                    MessagingErrorCode code = e.getMessagingErrorCode();
                    if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
                        tokens.deleteById(d.getId());         // the app was uninstalled or the token is dead
                    } else {
                        log.warn("Push to user {} failed: {}", userId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Push to user {} failed: {}", userId, e.getMessage());
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}

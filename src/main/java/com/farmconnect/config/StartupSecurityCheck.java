package com.farmconnect.config;

import com.farmconnect.model.Role;
import com.farmconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Shouts in the log when the server is running with the sample secrets from the repository. */
@Component
@Order(5)
public class StartupSecurityCheck implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupSecurityCheck.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.jwt.secret:}") private String jwtSecret;
    @Value("${app.crypto.key:}") private String cryptoKey;
    @Value("${spring.h2.console.enabled:false}") private boolean h2;

    public StartupSecurityCheck(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }

    @Override public void run(String... args) {
        try {
            if (jwtSecret.contains("DevelopmentOnly")) warn("app.jwt.secret is still the sample value. Anyone who has read the repository can forge a login for ANY account. Set a long random secret (JWT_SECRET environment variable or properties).");
            if (cryptoKey.startsWith("change-this")) warn("app.crypto.key is still the sample value. Stored sensitive fields are not properly protected.");
            if (h2) warn("The H2 database console is enabled. Turn it off (spring.h2.console.enabled=false) on any server people can reach.");
            boolean weak = users.findAll().stream().anyMatch(u -> u.getRole() == Role.ADMIN && encoder.matches("ChangeMe123!", u.getPassword()));
            if (weak) warn("A super admin account still has the default password. Change it now (Security > Change my password).");
        } catch (Exception ignored) { }
    }

    private static void warn(String msg) { log.warn("\n==================== SECURITY WARNING ====================\n{}\n==========================================================", msg); }
}

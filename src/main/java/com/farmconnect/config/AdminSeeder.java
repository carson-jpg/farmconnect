package com.farmconnect.config;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN account from application.properties (only if it does not exist yet). */
@Component
public class AdminSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email:}") private String email;
    @Value("${app.admin.password:}") private String password;
    @Value("${app.admin.name:FarmConnect Admin}") private String name;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override public void run(String... args) {
        if (email.isBlank() || password.isBlank() || users.existsByEmail(email)) return;
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRole(Role.ADMIN);
        users.save(u);
    }
}
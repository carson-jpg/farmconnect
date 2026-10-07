package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        // Only farmers and buyers can self-register. ADMIN is seeded, OFFICER is created by an admin.
        if (r.role() != Role.FARMER && r.role() != Role.BUYER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This role cannot be self-registered");
        if (users.existsByEmail(r.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setPhone(r.phone());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(r.role());
        users.save(u);
        return toResponse(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!u.isEnabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been suspended. Please contact FarmConnect support.");
        return toResponse(u);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal User u) {
        return new AuthResponse(null, u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generate(u), u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
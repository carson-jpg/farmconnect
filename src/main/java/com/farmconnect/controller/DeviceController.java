package com.farmconnect.controller;

import com.farmconnect.model.DeviceToken;
import com.farmconnect.model.User;
import com.farmconnect.repository.DeviceTokenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/** The app registers its Firebase token here after login, and removes it on logout. */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceTokenRepository tokens;

    public record TokenRequest(@NotBlank @Size(max = 512) String token, @Size(max = 20) String platform) {}

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody TokenRequest r, @AuthenticationPrincipal User me) {
        try {
            DeviceToken d = tokens.findByToken(r.token()).orElseGet(DeviceToken::new);
            d.setUser(me);                       // a phone that changes account follows the new account
            d.setToken(r.token());
            d.setPlatform(r.platform() == null || r.platform().isBlank() ? "android" : r.platform());
            d.setUpdatedAt(Instant.now());
            tokens.save(d);
        } catch (DataIntegrityViolationException e) {
            // two requests registered the same token at the same moment; the other one already saved it
        }
    }

    @DeleteMapping
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestParam String token, @AuthenticationPrincipal User me) {
        tokens.findByToken(token).filter(d -> d.getUser().getId().equals(me.getId())).ifPresent(tokens::delete);
    }
}
package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.FarmerVerification;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.model.VerificationStatus;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.service.FileStorageService;
import com.farmconnect.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Only ADMIN and County Agricultural OFFICER accounts can use these endpoints. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
@RequiredArgsConstructor
public class AdminVerificationController {
    private final VerificationService service;
    private final FileStorageService files;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @GetMapping("/verifications")
    public List<ReviewSummary> list(@RequestParam(defaultValue = "PENDING") VerificationStatus status) {
        return service.list(status).stream().map(Mapper::reviewSummary).toList();
    }

    @GetMapping("/verifications/{id}")
    public ReviewDetail detail(@PathVariable Long id) {
        return Mapper.reviewDetail(service.get(id));
    }

    @GetMapping("/verifications/{id}/documents/{type}")
    public ResponseEntity<byte[]> document(@PathVariable Long id, @PathVariable String type) {
        FarmerVerification v = service.get(id);
        String path = VerificationService.pathFor(v, type);
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not uploaded");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorageService.contentType(path)))
                .header("Cache-Control", "no-store").body(files.read(path));
    }

    @PostMapping("/verifications/{id}/start-review")
    public ReviewDetail start(@PathVariable Long id, @AuthenticationPrincipal User reviewer) {
        return Mapper.reviewDetail(service.startReview(id, reviewer));
    }

    @PostMapping("/verifications/{id}/approve")
    public ReviewDetail approve(@PathVariable Long id, @AuthenticationPrincipal User reviewer) {
        return Mapper.reviewDetail(service.approve(id, reviewer));
    }

    @PostMapping("/verifications/{id}/reject")
    public ReviewDetail reject(@PathVariable Long id, @Valid @RequestBody RejectRequest r,
                               @AuthenticationPrincipal User reviewer) {
        return Mapper.reviewDetail(service.reject(id, reviewer, r.reason()));
    }

    /** ADMIN only: create a County Agricultural Officer account. */
    @PostMapping("/officers")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createOfficer(@Valid @RequestBody OfficerRequest r) {
        if (users.existsByEmail(r.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setPhone(r.phone());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(Role.OFFICER);
        users.save(u);
        return new AuthResponse(null, u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
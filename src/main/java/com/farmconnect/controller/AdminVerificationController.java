package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.FarmerVerification;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.model.VerificationStatus;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.service.FileStorageService;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.Locations;
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
    public List<ReviewSummary> list(@RequestParam(defaultValue = "PENDING") VerificationStatus status,
                                    @AuthenticationPrincipal User me) {
        AreaScope scope = AreaScope.of(me);
        return service.list(status).stream().filter(v -> scope.allows(v.getSubCounty())).map(Mapper::reviewSummary).toList();
    }

    /** Officers only get to open farmers inside the area they were assigned. */
    private FarmerVerification inArea(Long id, User me) {
        FarmerVerification v = service.get(id);
        if (!AreaScope.of(me).allows(v.getSubCounty()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This farmer is outside your assigned area");
        return v;
    }

    @GetMapping("/verifications/{id}")
    public ReviewDetail detail(@PathVariable Long id, @AuthenticationPrincipal User me) {
        return Mapper.reviewDetail(inArea(id, me));
    }

    @GetMapping("/verifications/{id}/documents/{type}")
    public ResponseEntity<byte[]> document(@PathVariable Long id, @PathVariable String type, @AuthenticationPrincipal User me) {
        FarmerVerification v = inArea(id, me);
        String path = VerificationService.pathFor(v, type);
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not uploaded");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorageService.contentType(path)))
                .header("Cache-Control", "no-store").body(files.read(path));
    }

    @PostMapping("/verifications/{id}/start-review")
    public ReviewDetail start(@PathVariable Long id, @AuthenticationPrincipal User reviewer) {
        inArea(id, reviewer);
        return Mapper.reviewDetail(service.startReview(id, reviewer));
    }

    @PostMapping("/verifications/{id}/approve")
    public ReviewDetail approve(@PathVariable Long id, @AuthenticationPrincipal User reviewer) {
        inArea(id, reviewer);
        return Mapper.reviewDetail(service.approve(id, reviewer));
    }

    @PostMapping("/verifications/{id}/reject")
    public ReviewDetail reject(@PathVariable Long id, @Valid @RequestBody RejectRequest r,
                               @AuthenticationPrincipal User reviewer) {
        inArea(id, reviewer);
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
        assign(u, r.county(), r.subCounties());
        users.save(u);
        return new AuthResponse(null, u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    public record AssignmentRequest(String county, List<String> subCounties) {}

    /** ADMIN only: give an officer a county, and optionally only some of its sub-counties. Empty county = sees everything. */
    @PutMapping("/officers/{id}/assignment")
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary assignOfficer(@PathVariable Long id, @RequestBody AssignmentRequest r) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() != Role.OFFICER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only officers can be assigned an area");
        assign(u, r.county(), r.subCounties());
        return Mapper.user(users.save(u));
    }

    private static void assign(User u, String county, List<String> subCounties) {
        Locations.requireValidAssignment(county, subCounties);
        boolean none = county == null || county.isBlank();
        u.setAssignedCounty(none ? null : county);
        u.getAssignedSubCounties().clear();
        if (!none && subCounties != null) u.getAssignedSubCounties().addAll(subCounties);
    }
}
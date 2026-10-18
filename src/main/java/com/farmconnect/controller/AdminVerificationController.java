package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.FarmerVerification;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.model.VerificationStatus;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.Staff;
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

/**
 * Verification review and officer accounts. Super admin, county admin and county agricultural officer can review;
 * each only inside their area. The super admin creates officers anywhere; a county admin only inside their own county.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN','OFFICER')")
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

    /**
     * Create a County Agricultural Officer account. The super admin may put the officer in any county (or none yet).
     * A county admin can only create officers for their own county: whatever county is sent, theirs is used.
     */
    @PostMapping("/officers")
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse createOfficer(@Valid @RequestBody OfficerRequest r, @AuthenticationPrincipal User me) {
        if (users.existsByEmail(r.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        com.farmconnect.security.PasswordPolicy.require(r.password(), r.email());
        String county = r.county();
        if (Staff.isCountyAdmin(me)) county = ownCounty(me);
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setPhone(r.phone());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(Role.OFFICER);
        assign(u, county, r.subCounties());
        users.save(u);
        return new AuthResponse(null, u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    public record AssignmentRequest(String county, List<String> subCounties) {}

    /**
     * Give an officer a county, and optionally only some of its sub-counties. Empty county = sees everything.
     * A county admin can only change officers who already work in their county, and only within that county.
     */
    @PutMapping("/officers/{id}/assignment")
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
    public UserSummary assignOfficer(@PathVariable Long id, @RequestBody AssignmentRequest r, @AuthenticationPrincipal User me) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() != Role.OFFICER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only officers can be assigned an area");
        String county = r.county();
        if (Staff.isCountyAdmin(me)) {
            String mine = ownCounty(me);
            if (!mine.equals(u.getAssignedCounty()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This officer does not work in your county");
            county = mine;                       // cannot move an officer out of the county, or make them see everything
        }
        assign(u, county, r.subCounties());
        return Mapper.user(users.save(u));
    }

    /** The county a county admin is responsible for; a county admin without one cannot manage anyone yet. */
    private static String ownCounty(User me) {
        String c = me.getAssignedCounty();
        if (c == null || c.isBlank() || !Locations.isCounty(c))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No county has been assigned to you yet. Ask the super admin.");
        return c;
    }

    private static void assign(User u, String county, List<String> subCounties) {
        Locations.requireValidAssignment(county, subCounties);
        boolean none = county == null || county.isBlank();
        u.setAssignedCounty(none ? null : county);
        u.getAssignedSubCounties().clear();
        if (!none && subCounties != null) u.getAssignedSubCounties().addAll(subCounties);
    }
}

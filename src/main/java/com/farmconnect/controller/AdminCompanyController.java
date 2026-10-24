package com.farmconnect.controller;

import com.farmconnect.dto.CompanyDtos.*;
import com.farmconnect.model.CompanyDocument;
import com.farmconnect.model.CompanyStatus;
import com.farmconnect.model.User;
import com.farmconnect.security.ClientIp;
import com.farmconnect.service.AuditService;
import com.farmconnect.service.CompanyService;
import com.farmconnect.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Company verification. SUPER ADMIN ONLY: county admins and officers cannot see or change companies. */
@RestController
@RequestMapping("/api/admin/companies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCompanyController {
    private final CompanyService service;
    private final FileStorageService files;
    private final AuditService audit;

    @GetMapping
    public List<AdminCompanySummary> list(@RequestParam(defaultValue = "PENDING_VERIFICATION") CompanyStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public AdminCompanyDetail detail(@PathVariable Long id, @AuthenticationPrincipal User me) {
        return service.detail(me, id);
    }

    @GetMapping("/{id}/documents/{docId}")
    public ResponseEntity<byte[]> document(@PathVariable Long id, @PathVariable Long docId, @AuthenticationPrincipal User me) {
        CompanyDocument d = service.document(me, id, docId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorageService.contentType(d.getPath())))
                .header("Cache-Control", "no-store").body(files.read(d.getPath()));
    }

    @PostMapping("/{id}/start-review")
    public AdminCompanyDetail start(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.startReview(me, id);
        audit.record(me, "COMPANY_REVIEW_STARTED", d.company().name(), null, ClientIp.of(http), true);
        return d;
    }

    @PostMapping("/{id}/verify")
    public AdminCompanyDetail verify(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.verify(me, id);
        audit.record(me, "COMPANY_VERIFIED", d.company().name(), null, ClientIp.of(http), true);
        return d;
    }

    @PostMapping("/{id}/reject")
    public AdminCompanyDetail reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest r,
                                     @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.reject(me, id, r.reason());
        audit.record(me, "COMPANY_REJECTED", d.company().name(), r.reason(), ClientIp.of(http), true);
        return d;
    }

    @PostMapping("/{id}/suspend")
    public AdminCompanyDetail suspend(@PathVariable Long id, @Valid @RequestBody ReasonRequest r,
                                      @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.suspend(me, id, r.reason());
        audit.record(me, "COMPANY_SUSPENDED", d.company().name(), r.reason(), ClientIp.of(http), true);
        return d;
    }

    @PostMapping("/{id}/reinstate")
    public AdminCompanyDetail reinstate(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.reinstate(me, id);
        audit.record(me, "COMPANY_REINSTATED", d.company().name(), null, ClientIp.of(http), true);
        return d;
    }

    @PostMapping("/{id}/payout-account/approve")
    public AdminCompanyDetail approvePayout(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.approvePayout(me, id);
        audit.record(me, "COMPANY_PAYOUT_APPROVED", d.company().name(), d.company().payoutType() + " " + d.company().payoutAccount(), ClientIp.of(http), true);
        return d;
    }

    @PostMapping("/{id}/payout-account/revoke")
    public AdminCompanyDetail revokePayout(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletRequest http) {
        AdminCompanyDetail d = service.revokePayout(me, id);
        audit.record(me, "COMPANY_PAYOUT_REVOKED", d.company().name(), null, ClientIp.of(http), true);
        return d;
    }
}

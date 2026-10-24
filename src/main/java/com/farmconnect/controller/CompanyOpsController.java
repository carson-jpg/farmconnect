package com.farmconnect.controller;

import com.farmconnect.dto.CompanyDtos.ReasonRequest;
import com.farmconnect.dto.MarketDtos.*;
import com.farmconnect.model.ServiceRequestStatus;
import com.farmconnect.model.User;
import com.farmconnect.service.CompanyReportService;
import com.farmconnect.service.CompanyTeamService;
import com.farmconnect.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** The rest of the company portal: team, services, service requests, invoices and CSV reports. */
@RestController
@RequestMapping("/api/company")
@PreAuthorize("hasRole('COMPANY')")
@RequiredArgsConstructor
public class CompanyOpsController {
    private final CompanyTeamService team;
    private final MarketplaceService market;
    private final CompanyReportService reports;

    // ---- team
    @GetMapping("/team")
    public List<MemberView> team(@AuthenticationPrincipal User u) { return team.list(u); }

    @PostMapping("/team")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberView addMember(@Valid @RequestBody MemberForm f, @AuthenticationPrincipal User u) { return team.create(u, f); }

    @PutMapping("/team/{id}")
    public MemberView updateMember(@PathVariable Long id, @Valid @RequestBody MemberForm f, @AuthenticationPrincipal User u) { return team.update(u, id, f); }

    @DeleteMapping("/team/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable Long id, @AuthenticationPrincipal User u) { team.delete(u, id); }

    // ---- services
    @GetMapping("/services")
    public List<ServiceView> services(@AuthenticationPrincipal User u) { return market.myServices(u); }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceView createService(@Valid @RequestBody ServiceForm f, @AuthenticationPrincipal User u) { return market.create(u, f); }

    @PutMapping("/services/{id}")
    public ServiceView updateService(@PathVariable Long id, @Valid @RequestBody ServiceForm f, @AuthenticationPrincipal User u) { return market.update(u, id, f); }

    @DeleteMapping("/services/{id}")
    public ServiceView deactivateService(@PathVariable Long id, @AuthenticationPrincipal User u) { return market.deactivate(u, id); }

    // ---- service requests
    @GetMapping("/service-requests")
    public List<ServiceRequestView> serviceRequests(@RequestParam(required = false) ServiceRequestStatus status, @AuthenticationPrincipal User u) {
        return market.companyRequests(u, status);
    }

    @PostMapping("/service-requests/{id}/accept")
    public ServiceRequestView accept(@PathVariable Long id, @Valid @RequestBody(required = false) AcceptForm f, @AuthenticationPrincipal User u) { return market.accept(u, id, f); }

    @PostMapping("/service-requests/{id}/decline")
    public ServiceRequestView decline(@PathVariable Long id, @Valid @RequestBody ReasonRequest r, @AuthenticationPrincipal User u) { return market.decline(u, id, r.reason()); }

    @PostMapping("/service-requests/{id}/complete")
    public ServiceRequestView complete(@PathVariable Long id, @AuthenticationPrincipal User u) { return market.complete(u, id); }

    // ---- invoice and reports
    @GetMapping("/supply-orders/{id}/invoice")
    public Invoice invoice(@PathVariable Long id, @AuthenticationPrincipal User u) { return reports.invoice(u, id); }

    @GetMapping("/reports/sales.csv")
    public ResponseEntity<byte[]> sales(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @AuthenticationPrincipal User u) {
        return csv("farmconnect-sales.csv", reports.salesCsv(u, from, to));
    }

    @GetMapping("/reports/payouts.csv")
    public ResponseEntity<byte[]> payouts(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @AuthenticationPrincipal User u) {
        return csv("farmconnect-payouts.csv", reports.payoutsCsv(u, from, to));
    }

    @GetMapping("/reports/purchases.csv")
    public ResponseEntity<byte[]> purchases(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @AuthenticationPrincipal User u) {
        return csv("farmconnect-purchases.csv", reports.purchasesCsv(u, from, to));
    }

    private static ResponseEntity<byte[]> csv(String name, String body) {
        byte[] bytes = ("\uFEFF" + body).getBytes(StandardCharsets.UTF_8);          // BOM so Excel reads UTF-8
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"").header("Cache-Control", "no-store").body(bytes);
    }
}

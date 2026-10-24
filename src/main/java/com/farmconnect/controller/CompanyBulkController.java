package com.farmconnect.controller;

import com.farmconnect.dto.BulkDtos.*;
import com.farmconnect.dto.CompanyDtos.ReasonRequest;
import com.farmconnect.model.User;
import com.farmconnect.service.BulkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** A buying company's produce requests, the quotations it receives, and its purchase orders. */
@RestController
@RequestMapping("/api/company")
@PreAuthorize("hasRole('COMPANY')")
@RequiredArgsConstructor
public class CompanyBulkController {
    private final BulkService service;

    @GetMapping("/requests")
    public List<RequestView> requests(@AuthenticationPrincipal User u) { return service.myRequests(u); }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(@Valid @RequestBody RequestForm f, @AuthenticationPrincipal User u) { return service.create(u, f); }

    @PutMapping("/requests/{id}")
    public RequestView update(@PathVariable Long id, @Valid @RequestBody RequestForm f, @AuthenticationPrincipal User u) { return service.update(u, id, f); }

    @PostMapping("/requests/{id}/close")
    public RequestView close(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.close(u, id); }

    @GetMapping("/requests/{id}/quotations")
    public List<QuoteView> quotations(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.quotationsFor(u, id); }

    @PostMapping("/quotations/{id}/accept")
    public PurchaseOrderView accept(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.accept(u, id); }

    @PostMapping("/quotations/{id}/reject")
    public QuoteView reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, @AuthenticationPrincipal User u) {
        return service.reject(u, id, body == null ? null : body.get("reason"));
    }

    @GetMapping("/purchase-orders")
    public List<PurchaseOrderView> orders(@AuthenticationPrincipal User u) { return service.companyOrders(u); }

    @PostMapping("/purchase-orders/{id}/deliver")
    public PurchaseOrderView deliver(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.deliver(u, id); }

    @PostMapping("/purchase-orders/{id}/mark-paid")
    public PurchaseOrderView markPaid(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.markPaid(u, id); }

    @PostMapping("/purchase-orders/{id}/cancel")
    public PurchaseOrderView cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest r, @AuthenticationPrincipal User u) {
        return service.cancel(u, id, r.reason());
    }
}

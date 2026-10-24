package com.farmconnect.controller;

import com.farmconnect.dto.CompanyDtos.CompanyView;
import com.farmconnect.dto.CompanyDtos.PayoutAccountRequest;
import com.farmconnect.dto.CompanyDtos.ReasonRequest;
import com.farmconnect.dto.SupplyDtos.*;
import com.farmconnect.model.SupplyOrderStatus;
import com.farmconnect.model.User;
import com.farmconnect.service.CompanyService;
import com.farmconnect.service.SupplyPayoutService;
import com.farmconnect.service.SupplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A company's listings, orders and sales summary (web and Android portals). Listing needs a VERIFIED company. */
@RestController
@RequestMapping("/api/company")
@PreAuthorize("hasRole('COMPANY')")
@RequiredArgsConstructor
public class CompanySupplyController {
    private final SupplyService service;
    private final CompanyService companyService;
    private final SupplyPayoutService payouts;

    @GetMapping("/products")
    public List<ProductView> products(@AuthenticationPrincipal User u) { return service.myProducts(u); }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody ProductRequest r, @AuthenticationPrincipal User u) { return service.createProduct(u, r); }

    @PutMapping("/products/{id}")
    public ProductView update(@PathVariable Long id, @Valid @RequestBody ProductRequest r, @AuthenticationPrincipal User u) {
        return service.updateProduct(u, id, r);
    }

    @DeleteMapping("/products/{id}")
    public ProductView deactivate(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.deactivate(u, id); }

    @GetMapping("/supply-orders")
    public List<OrderView> orders(@RequestParam(required = false) SupplyOrderStatus status, @AuthenticationPrincipal User u) {
        return service.companyOrders(u, status);
    }

    @GetMapping("/supply-orders/{id}")
    public OrderView order(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.companyOrder(u, id); }

    @PostMapping("/supply-orders/{id}/confirm")
    public OrderView confirm(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.confirm(u, id); }

    @PostMapping("/supply-orders/{id}/dispatch")
    public OrderView dispatch(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.dispatch(u, id); }

    @PostMapping("/supply-orders/{id}/deliver")
    public OrderView deliver(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.deliver(u, id); }

    @PostMapping("/supply-orders/{id}/reject")
    public OrderView reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest r, @AuthenticationPrincipal User u) {
        return service.reject(u, id, r.reason());
    }

    @PostMapping("/supply-orders/{id}/mark-paid")
    public OrderView markPaid(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.markPaid(u, id); }

    @GetMapping("/summary")
    public CompanySummary summary(@AuthenticationPrincipal User u) { return service.summary(u); }

    /** Where FarmConnect sends this company's money. Needs the super admin's approval after every change. */
    @PutMapping("/payout-account")
    public CompanyView payoutAccount(@Valid @RequestBody PayoutAccountRequest r, @AuthenticationPrincipal User u) {
        return companyService.setPayoutAccount(u, r);
    }

    @GetMapping("/payouts")
    public List<PayoutView> payouts(@AuthenticationPrincipal User u) {
        return payouts.forCompany(companyService.entityOf(u).getId());
    }
}

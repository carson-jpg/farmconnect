package com.farmconnect.controller;

import com.farmconnect.dto.CashDtos.*;
import com.farmconnect.model.User;
import com.farmconnect.service.CashPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Cash payments recorded in the app, approved by the super admin, and the commission bills they create. */
@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashPaymentService cash;

    /** A farmer sees the cash sales they recorded; a buyer sees the ones recorded against their orders. */
    @GetMapping
    @PreAuthorize("hasAnyRole('FARMER','BUYER')")
    public List<CashView> mine(@AuthenticationPrincipal User me) {
        return me.getRole() == com.farmconnect.model.Role.FARMER ? cash.forFarmer(me.getId()) : cash.forBuyer(me.getId());
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('FARMER','BUYER','ADMIN')")
    public List<CashView> forOrder(@PathVariable Long orderId, @AuthenticationPrincipal User me) { return cash.forOrder(orderId, me); }

    @PostMapping("/orders/{orderId}/record")
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CashView record(@PathVariable Long orderId, @RequestBody(required = false) RecordRequest r, @AuthenticationPrincipal User me) {
        return cash.record(orderId, me, r == null ? null : r.note());
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('BUYER')")
    public CashView confirm(@PathVariable Long id, @RequestBody(required = false) RespondRequest r, @AuthenticationPrincipal User me) {
        return cash.respond(id, me, true, r == null ? null : r.note());
    }

    @PostMapping("/{id}/dispute")
    @PreAuthorize("hasRole('BUYER')")
    public CashView dispute(@PathVariable Long id, @RequestBody RespondRequest r, @AuthenticationPrincipal User me) {
        return cash.respond(id, me, false, r.note());
    }

    // ---- commission bills (farmer)

    @GetMapping("/bills")
    @PreAuthorize("hasRole('FARMER')")
    public List<BillView> bills(@AuthenticationPrincipal User me) { return cash.myBills(me.getId()); }

    @PostMapping("/bills/{id}/pay")
    @PreAuthorize("hasRole('FARMER')")
    public BillView pay(@PathVariable Long id, @RequestBody(required = false) PayBillRequest r, @AuthenticationPrincipal User me) {
        return cash.payBill(id, me, r == null ? null : r.phone());
    }

    // ---- super admin only

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CashView> adminList(@RequestParam(required = false) String status) { return cash.adminList(status); }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public CashView approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest r, @AuthenticationPrincipal User me) {
        return cash.approve(id, me, r == null ? null : r.note());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public CashView reject(@PathVariable Long id, @RequestBody DecisionRequest r, @AuthenticationPrincipal User me) {
        return cash.reject(id, me, r.note());
    }

    @GetMapping("/admin/bills")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BillView> adminBills(@RequestParam(required = false) String status) { return cash.adminBills(status); }

    @PostMapping("/bills/{id}/mark-paid")
    @PreAuthorize("hasRole('ADMIN')")
    public BillView markPaid(@PathVariable Long id, @RequestBody(required = false) MarkPaidRequest r, @AuthenticationPrincipal User me) {
        return cash.markPaid(id, me, r == null ? null : r.note());
    }
}

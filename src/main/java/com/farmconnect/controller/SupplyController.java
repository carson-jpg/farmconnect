package com.farmconnect.controller;

import com.farmconnect.dto.SupplyDtos.*;
import com.farmconnect.model.SupplyCategory;
import com.farmconnect.model.User;
import com.farmconnect.service.SupplyPaymentService;
import com.farmconnect.service.SupplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Farmers buy inputs (seed, fertiliser, feed, equipment) from verified companies. */
@RestController
@RequestMapping("/api/supplies")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class SupplyController {
    private final SupplyService service;
    private final SupplyPaymentService payments;

    @GetMapping("/products")
    public List<ProductView> browse(@RequestParam(required = false) SupplyCategory category, @RequestParam(required = false) String q) {
        return service.browse(category, q);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@Valid @RequestBody PlaceOrderRequest r, @AuthenticationPrincipal User u) { return service.place(u, r); }

    @GetMapping("/orders")
    public List<OrderView> mine(@AuthenticationPrincipal User u) { return service.myOrders(u); }

    @GetMapping("/orders/{id}")
    public OrderView one(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.myOrder(u, id); }

    /** Sends an M-Pesa prompt for a DELIVERED order. The amount always comes from the server. */
    @PostMapping("/orders/{id}/pay")
    public SupplyPaymentView pay(@PathVariable Long id, @Valid @RequestBody PayRequest r, @AuthenticationPrincipal User u) {
        return payments.startStk(u, id, r.phone());
    }

    /** Whether M-Pesa payment for farm inputs is switched on (the app hides the button when it is not). */
    @GetMapping("/payments-enabled")
    public java.util.Map<String, Boolean> enabled() { return java.util.Map.of("enabled", payments.enabled()); }

    @PostMapping("/orders/{id}/cancel")
    public OrderView cancel(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.cancel(u, id); }
}

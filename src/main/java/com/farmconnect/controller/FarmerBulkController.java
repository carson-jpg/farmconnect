package com.farmconnect.controller;

import com.farmconnect.dto.BulkDtos.*;
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

/** Farmers answer companies' bulk requests with quotations and confirm the purchase orders that result. */
@RestController
@RequestMapping("/api/bulk")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class FarmerBulkController {
    private final BulkService service;

    @GetMapping("/requests")
    public List<RequestView> browse(@RequestParam(required = false) String produce, @RequestParam(required = false) String county) {
        return service.browse(produce, county);
    }

    @PostMapping("/requests/{id}/quotations")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteView quote(@PathVariable Long id, @Valid @RequestBody QuoteForm f, @AuthenticationPrincipal User u) { return service.quote(u, id, f); }

    @GetMapping("/quotations")
    public List<QuoteView> mine(@AuthenticationPrincipal User u) { return service.myQuotations(u); }

    @PostMapping("/quotations/{id}/withdraw")
    public QuoteView withdraw(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.withdraw(u, id); }

    @GetMapping("/purchase-orders")
    public List<PurchaseOrderView> orders(@AuthenticationPrincipal User u) { return service.farmerOrders(u); }

    @PostMapping("/purchase-orders/{id}/confirm")
    public PurchaseOrderView confirm(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.confirm(u, id); }

    @PostMapping("/purchase-orders/{id}/decline")
    public PurchaseOrderView decline(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, @AuthenticationPrincipal User u) {
        return service.decline(u, id, body == null ? null : body.get("reason"));
    }
}

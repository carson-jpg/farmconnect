package com.farmconnect.controller;

import com.farmconnect.dto.MarketDtos.*;
import com.farmconnect.model.ServiceCategory;
import com.farmconnect.model.User;
import com.farmconnect.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Farmers find and request services from verified companies (transport, machinery hire, soil testing...). */
@RestController
@RequestMapping("/api/marketplace")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class MarketplaceController {
    private final MarketplaceService service;

    @GetMapping("/services")
    public List<ServiceView> browse(@RequestParam(required = false) ServiceCategory category, @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String county) {
        return service.browse(category, q, county);
    }

    @PostMapping("/services/{id}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestView request(@PathVariable Long id, @Valid @RequestBody ServiceRequestForm f, @AuthenticationPrincipal User u) {
        return service.request(u, id, f);
    }

    @GetMapping("/service-requests")
    public List<ServiceRequestView> mine(@AuthenticationPrincipal User u) { return service.myRequests(u); }

    @PostMapping("/service-requests/{id}/cancel")
    public ServiceRequestView cancel(@PathVariable Long id, @AuthenticationPrincipal User u) { return service.cancel(u, id); }
}

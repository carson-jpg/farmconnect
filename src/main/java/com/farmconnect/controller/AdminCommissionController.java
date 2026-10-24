package com.farmconnect.controller;

import com.farmconnect.dto.SupplyDtos.PolicyRequest;
import com.farmconnect.dto.SupplyDtos.PolicyView;
import com.farmconnect.model.CommissionType;
import com.farmconnect.model.User;
import com.farmconnect.security.ClientIp;
import com.farmconnect.service.AuditService;
import com.farmconnect.service.CommissionPolicyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Commission rates per transaction type. SUPER ADMIN ONLY. A change applies to new orders; old orders keep their rate. */
@RestController
@RequestMapping("/api/admin/commission-policies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCommissionController {
    private final CommissionPolicyService service;
    private final AuditService audit;

    @GetMapping
    public List<PolicyView> current() { return service.currentAll(); }

    @GetMapping("/{type}/history")
    public List<PolicyView> history(@PathVariable CommissionType type) { return service.history(type); }

    @PostMapping
    public PolicyView set(@Valid @RequestBody PolicyRequest r, @AuthenticationPrincipal User me, HttpServletRequest http) {
        PolicyView v = service.set(me, r);
        audit.record(me, "COMMISSION_POLICY_SET", r.type().name(),
                "platform " + r.platformBps() + " bps, county " + r.countyBps() + " bps", ClientIp.of(http), true);
        return v;
    }
}

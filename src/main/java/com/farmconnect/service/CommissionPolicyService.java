package com.farmconnect.service;

import com.farmconnect.dto.SupplyDtos.PolicyRequest;
import com.farmconnect.dto.SupplyDtos.PolicyView;
import com.farmconnect.model.CommissionPolicy;
import com.farmconnect.model.CommissionType;
import com.farmconnect.model.User;
import com.farmconnect.repository.CommissionPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Super admin sets the rate per transaction type. Orders copy the rate in force when they are placed. */
@Service
@RequiredArgsConstructor
public class CommissionPolicyService {
    private final CommissionPolicyRepository policies;

    /** The policy in force now. A first-use default (5% FarmConnect, 0% county) is created for SUPPLY_SALE if nobody set one. */
    @Transactional
    public CommissionPolicy current(CommissionType type) {
        return policies.current(type, Instant.now()).orElseGet(() -> {
            CommissionPolicy p = new CommissionPolicy();
            p.setType(type);
            p.setPlatformBps(type == CommissionType.SUPPLY_SALE ? 500 : 0);
            p.setCountyBps(0);
            p.setNote("Default. The super admin can change it.");
            return policies.save(p);
        });
    }

    @Transactional
    public List<PolicyView> currentAll() {
        return Arrays.stream(CommissionType.values()).map(t -> view(current(t))).toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyView> history(CommissionType type) {
        return policies.findByTypeOrderByEffectiveFromDesc(type).stream().map(CommissionPolicyService::view).toList();
    }

    @Transactional
    public PolicyView set(User admin, PolicyRequest r) {
        if (r.platformBps() + r.countyBps() > 10_000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The shares add up to more than 100%");
        if (r.countyBps() > 0 && (r.countyAgreementRef() == null || r.countyAgreementRef().isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A county share needs the reference of the formally approved agreement");
        Instant now = Instant.now();
        policies.inForce(r.type(), now).forEach(old -> { old.setEffectiveTo(now); policies.save(old); });
        CommissionPolicy p = new CommissionPolicy();
        p.setType(r.type());
        p.setPlatformBps(r.platformBps());
        p.setCountyBps(r.countyBps());
        p.setCountyAgreementRef(r.countyBps() > 0 ? r.countyAgreementRef().trim() : null);
        p.setEffectiveFrom(now);
        p.setCreatedBy(admin);
        p.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        return view(policies.save(p));
    }

    static PolicyView view(CommissionPolicy p) {
        return new PolicyView(p.getId(), p.getType(), p.getPlatformBps(), p.getCountyBps(), p.getCountyAgreementRef(),
                p.getEffectiveFrom(), p.getEffectiveTo(), p.getCreatedBy() == null ? null : p.getCreatedBy().getName(), p.getNote());
    }
}

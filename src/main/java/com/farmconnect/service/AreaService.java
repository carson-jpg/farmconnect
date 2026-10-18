package com.farmconnect.service;

import com.farmconnect.model.*;
import com.farmconnect.repository.FarmRepository;
import com.farmconnect.repository.FarmerVerificationRepository;
import com.farmconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Works out which farmers, orders and products fall inside a staff member's {@link AreaScope}.
 * A farmer belongs to the sub-county of their first farm that has one, otherwise the one on their verification.
 * Callers that read orders or products must run inside a transaction (the items are lazy).
 */
@Service
@RequiredArgsConstructor
public class AreaService {
    private final FarmRepository farms;
    private final FarmerVerificationRepository verifications;
    private final UserRepository users;

    /** farmer id -> sub-county. */
    public Map<Long, String> farmerSubCounties() {
        Map<Long, String> m = new HashMap<>();
        for (Farm f : farms.findAll()) {
            if (f.getSubCounty() != null && !f.getSubCounty().isBlank()) m.putIfAbsent(f.getOwner().getId(), f.getSubCounty());
        }
        for (FarmerVerification v : verifications.findAll()) {
            if (v.getSubCounty() != null && !v.getSubCounty().isBlank()) m.putIfAbsent(v.getFarmer().getId(), v.getSubCounty());
        }
        return m;
    }

    /** Ids of the farmers inside the area, or null when the area is "everything" (so callers can skip filtering). */
    public Set<Long> farmerIdsIn(AreaScope sc) {
        if (sc.all()) return null;
        Set<Long> ids = new HashSet<>();
        if (sc.none()) return ids;
        Map<Long, String> sub = farmerSubCounties();
        for (User u : users.findAll()) {
            if (u.getRole() == Role.FARMER && sc.allows(sub.get(u.getId()))) ids.add(u.getId());
        }
        return ids;
    }

    /** Is this farmer inside the area? */
    public boolean farmerIn(AreaScope sc, Long farmerId) {
        Set<Long> ids = farmerIdsIn(sc);
        return ids == null || ids.contains(farmerId);
    }

    /** At least one item in the order comes from a farmer in the area. (null ids = everything.) */
    public static boolean orderTouches(CustomerOrder o, Set<Long> farmerIds) {
        if (farmerIds == null) return true;
        for (OrderItem i : o.getItems()) if (farmerIds.contains(i.getProduct().getFarm().getOwner().getId())) return true;
        return false;
    }

    /** EVERY item in the order comes from farmers in the area, so changing the order cannot affect another county. */
    public static boolean orderWithin(CustomerOrder o, Set<Long> farmerIds) {
        if (farmerIds == null) return true;
        if (o.getItems().isEmpty()) return false;
        for (OrderItem i : o.getItems()) if (!farmerIds.contains(i.getProduct().getFarm().getOwner().getId())) return false;
        return true;
    }

    /** What the area's farmers earn from one order. (null ids = the whole order.) */
    public static BigDecimal areaTotal(CustomerOrder o, Set<Long> farmerIds) {
        if (farmerIds == null) return o.getTotal();
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem i : o.getItems()) {
            if (farmerIds.contains(i.getProduct().getFarm().getOwner().getId()))
                sum = sum.add(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }
        return sum;
    }
}

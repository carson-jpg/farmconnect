package com.farmconnect.payment.service;

import com.farmconnect.model.CustomerOrder;
import com.farmconnect.model.OrderItem;
import com.farmconnect.payment.model.*;
import com.farmconnect.payment.repository.AllocationRepository;
import com.farmconnect.payment.service.AllocationCalculator.FarmerShare;
import com.farmconnect.payment.service.AllocationCalculator.FarmerWeight;
import com.farmconnect.payment.service.AllocationCalculator.Split;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Applies the 5% / 5% / 90% rule and records the three entitlements plus their ledger lines.
 * Shares are worked out in WHOLE SHILLINGS (M-Pesa only moves whole shillings), then stored as cents.
 */
@Service
@RequiredArgsConstructor
public class CommissionService {
    private final PaymentPolicy policy;
    private final AllocationRepository allocations;
    private final LedgerService ledger;

    /** Pure calculation, no database writes. saleCents must be a whole number of shillings. */
    public Split calculate(CustomerOrder order, long saleCents) {
        if (saleCents % 100 != 0) throw new IllegalArgumentException("sale must be whole shillings");
        Map<Long, Long> perFarmer = new TreeMap<>();   // sorted by farmer id: same input, same output
        for (OrderItem i : order.getItems()) {
            long farmerId = i.getProduct().getFarm().getOwner().getId();
            long cents = i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                    .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            perFarmer.merge(farmerId, cents, Long::sum);
        }
        List<FarmerWeight> weights = new ArrayList<>();
        perFarmer.forEach((id, c) -> weights.add(new FarmerWeight(id, c)));
        Split inShillings = AllocationCalculator.split(saleCents / 100, policy.getPlatformBps(), policy.getCountyBps(), weights);
        List<FarmerShare> farmers = inShillings.farmers().stream()
                .map(f -> new FarmerShare(f.farmerId(), f.amountCents() * 100)).toList();
        return new Split(inShillings.platformCents() * 100, inShillings.countyCents() * 100, farmers);
    }

    /** Creates the allocations and the ALLOCATION ledger lines. The total must equal saleCents exactly. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PaymentAllocation> allocate(Payment payment, CustomerOrder order, long saleCents) {
        Split s = calculate(order, saleCents);
        if (s.total() != saleCents) throw new IllegalStateException("Allocation total " + s.total() + " != sale " + saleCents);

        List<PaymentAllocation> out = new ArrayList<>();
        out.add(create(payment, BeneficiaryType.PLATFORM, "FARMCONNECT", policy.getPlatformBps(), s.platformCents()));
        out.add(create(payment, BeneficiaryType.COUNTY, policy.getCountyName(), policy.getCountyBps(), s.countyCents()));
        for (FarmerShare f : s.farmers())
            out.add(create(payment, BeneficiaryType.FARMER, String.valueOf(f.farmerId()),
                    (int) (f.amountCents() * 10_000 / saleCents), f.amountCents()));
        out.removeIf(a -> a == null);
        return out;
    }

    private PaymentAllocation create(Payment p, BeneficiaryType type, String id, int bps, long cents) {
        if (cents <= 0) return null;
        PaymentAllocation a = new PaymentAllocation();
        a.setPayment(p);
        a.setBeneficiaryType(type);
        a.setBeneficiaryId(id);
        a.setBasisPoints(bps);
        a.setAmountCents(cents);
        a.setStatus(AllocationStatus.RECORDED);
        a = allocations.save(a);
        ledger.move(p.getId(), a.getId(), "ALLOCATION", LedgerService.COLLECTED, LedgerService.payable(type, id), cents,
                "receipt " + p.getMpesaReceiptNumber());
        return a;
    }
}

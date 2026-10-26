package com.farmconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.farmconnect.dto.CashDtos.*;
import com.farmconnect.model.*;
import com.farmconnect.payment.service.PaymentPolicy;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Cash sales recorded in the app. Flow: farmer records -> buyer confirms or disputes -> super admin approves or rejects.
 * Approval marks the order paid and creates a commission bill (the 5% platform + 5% county share) that the farmer pays
 * by M-Pesa in the app. The farmer keeps the rest, which he or she already received in cash.
 */
@Service
@RequiredArgsConstructor
public class CashPaymentService {
    private static final Logger log = LoggerFactory.getLogger(CashPaymentService.class);
    private static final Set<String> ACTIVE = Set.of(CashSale.RECORDED, CashSale.BUYER_CONFIRMED, CashSale.BUYER_DISPUTED, CashSale.APPROVED);
    private static final Set<String> OPEN_BILL = Set.of(CommissionBill.UNPAID, CommissionBill.PENDING);

    private final CashSaleRepository sales;
    private final CommissionBillRepository bills;
    private final OrderRepository orders;
    private final UserRepository users;
    private final PaymentPolicy policy;
    private final com.farmconnect.service.MpesaService mpesa;
    private final NotificationService notifier;

    /** A farmer with a bill unpaid for this many days cannot record new cash sales. */
    @Value("${app.cash.max-overdue-days:7}") private int maxOverdueDays;

    // ------------------------------------------------------------------ reading

    @Transactional(readOnly = true)
    public List<CashView> forFarmer(Long farmerId) { return views(sales.findByFarmerIdOrderByRecordedAtDesc(farmerId)); }

    @Transactional(readOnly = true)
    public List<CashView> forBuyer(Long buyerId) { return views(sales.findByOrderBuyerIdOrderByRecordedAtDesc(buyerId)); }

    @Transactional(readOnly = true)
    public List<CashView> forOrder(Long orderId, User me) {
        CustomerOrder o = order(orderId);
        boolean buyer = o.getBuyer().getId().equals(me.getId());
        boolean farmer = ownerIds(o).contains(me.getId());
        if (!buyer && !farmer && me.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        List<CashSale> all = sales.findByOrderIdOrderByRecordedAtAsc(orderId);
        if (!buyer && me.getRole() != Role.ADMIN) all = all.stream().filter(c -> c.getFarmer().getId().equals(me.getId())).toList();
        return views(all);
    }

    /** Super admin: awaiting a decision first, then recent history. */
    @Transactional(readOnly = true)
    public List<CashView> adminList(String status) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
            return views(sales.findByStatusInOrderByRecordedAtAsc(status.equalsIgnoreCase("PENDING")
                    ? List.of(CashSale.RECORDED, CashSale.BUYER_CONFIRMED, CashSale.BUYER_DISPUTED) : List.of(status.toUpperCase())));
        return views(sales.findTop300ByOrderByRecordedAtDesc());
    }

    @Transactional(readOnly = true)
    public List<BillView> myBills(Long farmerId) { return bills.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream().map(this::bill).toList(); }

    @Transactional(readOnly = true)
    public List<BillView> adminBills(String status) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
            return bills.findByStatusInOrderByCreatedAtAsc(status.equalsIgnoreCase("OPEN") ? OPEN_BILL : List.of(status.toUpperCase())).stream().map(this::bill).toList();
        return bills.findTop300ByOrderByCreatedAtDesc().stream().map(this::bill).toList();
    }

    // ------------------------------------------------------------------ farmer records

    @Transactional
    public CashView record(Long orderId, User farmer, String note) {
        CustomerOrder o = order(orderId);
        if (!ownerIds(o).contains(farmer.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        if ("MPESA_ONLINE".equals(o.getPaymentMethod()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is paid by M-Pesa in the app, not cash");
        if (o.getStatus() == OrderStatus.CANCELLED) throw new ResponseStatusException(HttpStatus.CONFLICT, "This order was cancelled");
        if (sales.existsByOrderIdAndFarmerIdAndStatusIn(orderId, farmer.getId(), ACTIVE))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already recorded cash for this order");
        Instant now = Instant.now();
        for (CommissionBill b : bills.findByFarmerIdAndStatusIn(farmer.getId(), OPEN_BILL))
            if (CashMath.overdue(b.getCreatedAt(), now, maxOverdueDays))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Pay your unpaid commission first (bill #" + b.getId() + ", KES " + kes(b.getTotalCents()) + ")");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem i : o.getItems())
            if (i.getProduct().getFarm().getOwner().getId().equals(farmer.getId()))
                subtotal = subtotal.add(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));

        CashSale c = new CashSale();
        c.setOrder(o);
        c.setFarmer(farmer);
        c.setAmountCents(CashMath.expectedCents(subtotal));
        c.setFarmerNote(clip(note));
        c = sales.save(c);
        notifier.notifyAndSms(o.getBuyer(), "CASH", "Please confirm your cash payment",
                farmer.getName() + " says you paid KES " + kes(c.getAmountCents()) + " in cash for order #" + o.getId() + ". Open the app and confirm or dispute it.", c.getId());
        return view(c);
    }

    // ------------------------------------------------------------------ buyer responds

    @Transactional
    public CashView respond(Long id, User buyer, boolean confirm, String note) {
        CashSale c = sale(id);
        if (!c.getOrder().getBuyer().getId().equals(buyer.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        if (!CashSale.RECORDED.equals(c.getStatus()) && !CashSale.BUYER_CONFIRMED.equals(c.getStatus()) && !CashSale.BUYER_DISPUTED.equals(c.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This cash payment is already " + c.getStatus().toLowerCase());
        if (!confirm && (note == null || note.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tell us what is wrong");
        c.setStatus(confirm ? CashSale.BUYER_CONFIRMED : CashSale.BUYER_DISPUTED);
        c.setBuyerNote(clip(note));
        c.setBuyerRespondedAt(Instant.now());
        c = sales.save(c);
        notifier.notify(c.getFarmer(), "CASH", confirm ? "Buyer confirmed the cash payment" : "Buyer disputed the cash payment",
                "Order #" + c.getOrder().getId() + (confirm ? ": waiting for the super admin to approve." : ": " + c.getBuyerNote()), c.getId());
        notifyAdmins("Cash payment " + (confirm ? "confirmed" : "DISPUTED") + " - order #" + c.getOrder().getId(),
                c.getFarmer().getName() + ", KES " + kes(c.getAmountCents()) + ". Review it in Cash approvals.", c.getId());
        return view(c);
    }

    // ------------------------------------------------------------------ super admin decides

    @Transactional
    public CashView approve(Long id, User admin, String note) {
        CashSale c = sale(id);
        if (CashSale.APPROVED.equals(c.getStatus())) return view(c);
        if (CashSale.REJECTED.equals(c.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This cash payment was rejected");
        CustomerOrder o = c.getOrder();
        if (o.getStatus() == OrderStatus.CANCELLED) throw new ResponseStatusException(HttpStatus.CONFLICT, "The order was cancelled");
        CashMath.Parts p = CashMath.split(c.getAmountCents(), policy.getPlatformBps(), policy.getCountyBps());
        c.setStatus(CashSale.APPROVED);
        c.setDecidedBy(admin.getId());
        c.setDecidedAt(Instant.now());
        c.setDecisionNote(clip(note));
        c = sales.save(c);

        CommissionBill b = new CommissionBill();
        b.setCashSaleId(c.getId());
        b.setFarmer(c.getFarmer());
        b.setOrderId(o.getId());
        b.setSaleCents(p.saleCents());
        b.setPlatformCents(p.platformCents());
        b.setCountyCents(p.countyCents());
        b.setTotalCents(p.billCents());
        b.setPhone(c.getFarmer().getPhone());
        bills.save(b);

        boolean allPaid = true;
        for (Long fid : ownerIds(o)) {
            boolean ok = sales.findByOrderIdOrderByRecordedAtAsc(o.getId()).stream()
                    .anyMatch(x -> x.getFarmer().getId().equals(fid) && CashSale.APPROVED.equals(x.getStatus()));
            if (!ok) { allPaid = false; break; }
        }
        if (allPaid) {
            o.setPaymentStatus("PAID");
            o.setUpdatedAt(Instant.now());
            orders.save(o);
        }
        notifier.notifyAndSms(c.getFarmer(), "CASH", "Cash payment approved - pay your commission",
                "Order #" + o.getId() + " is paid. Please pay the commission of KES " + kes(b.getTotalCents()) + " in the app (Cash payments).", b.getId());
        notifier.notify(o.getBuyer(), "ORDER", "Payment approved for order #" + o.getId(), "Your cash payment was approved. Thank you!", o.getId());
        return view(c);
    }

    @Transactional
    public CashView reject(Long id, User admin, String reason) {
        CashSale c = sale(id);
        if (CashSale.APPROVED.equals(c.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already approved");
        if (CashSale.REJECTED.equals(c.getStatus())) return view(c);
        if (reason == null || reason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give a reason");
        c.setStatus(CashSale.REJECTED);
        c.setDecidedBy(admin.getId());
        c.setDecidedAt(Instant.now());
        c.setDecisionNote(clip(reason));
        c = sales.save(c);
        String msg = "Cash payment for order #" + c.getOrder().getId() + " was not approved: " + c.getDecisionNote();
        notifier.notifyAndSms(c.getFarmer(), "CASH", "Cash payment rejected", msg, c.getId());
        notifier.notify(c.getOrder().getBuyer(), "CASH", "Cash payment rejected", msg, c.getId());
        return view(c);
    }

    // ------------------------------------------------------------------ commission bills

    /** Farmer: send the M-Pesa prompt for a bill. The amount always comes from the bill on the server. */
    @Transactional
    public BillView payBill(Long billId, User farmer, String phoneRaw) {
        CommissionBill b = bills.lockById(billId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        if (!b.getFarmer().getId().equals(farmer.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your bill");
        if (CommissionBill.PAID.equals(b.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already paid");
        if (CommissionBill.PENDING.equals(b.getStatus()) && b.getStkSentAt() != null
                && b.getStkSentAt().isAfter(Instant.now().minus(Duration.ofMinutes(2))))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prompt was just sent to your phone. Wait a moment, then try again");
        String raw = phoneRaw != null && !phoneRaw.isBlank() ? phoneRaw : farmer.getPhone();
        String phone = MpesaService.normalizePhone(raw);
        MpesaService.StkResult r = mpesa.stkPushFull(phone, BigDecimal.valueOf(b.getTotalCents(), 2), "CB" + b.getId(), "FarmConnect");
        b.setCheckoutRequestId(r.checkoutRequestId());
        b.setPhone(phone);
        b.setStatus(CommissionBill.PENDING);
        b.setStkSentAt(Instant.now());
        return bill(bills.save(b));
    }

    /** Super admin: the commission was received another way (for example straight to the till). */
    @Transactional
    public BillView markPaid(Long billId, User admin, String note) {
        CommissionBill b = bills.lockById(billId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
        if (CommissionBill.PAID.equals(b.getStatus())) return bill(b);
        b.setStatus(CommissionBill.PAID);
        b.setPaidVia("ADMIN");
        b.setPaidAt(Instant.now());
        b.setNote(clip((note == null ? "" : note + " | ") + "marked paid by admin #" + admin.getId()));
        b = bills.save(b);
        notifier.notify(b.getFarmer(), "CASH", "Commission received", "Bill #" + b.getId() + " (KES " + kes(b.getTotalCents()) + ") is marked paid. Thank you!", b.getId());
        return bill(b);
    }

    /** Called from the shared M-Pesa callback. True when the callback belonged to a commission bill. */
    @Transactional
    public boolean onStkCallback(JsonNode body) {
        JsonNode cb = body.path("Body").path("stkCallback");
        String checkoutId = cb.path("CheckoutRequestID").asText("");
        if (checkoutId.isBlank()) return false;
        CommissionBill b = bills.lockByCheckoutRequestId(checkoutId).orElse(null);
        if (b == null) return false;
        if (CommissionBill.PAID.equals(b.getStatus())) return true;               // Safaricom retries: stay idempotent
        if (cb.path("ResultCode").asInt(-1) != 0) {
            b.setStatus(CommissionBill.UNPAID);
            b.setNote(clip(cb.path("ResultDesc").asText("Payment not completed")));
            bills.save(b);
            return true;
        }
        String receipt = null;
        BigDecimal amount = null;
        for (JsonNode it : cb.path("CallbackMetadata").path("Item")) {
            String n = it.path("Name").asText();
            if (n.equals("MpesaReceiptNumber")) receipt = it.path("Value").asText(null);
            if (n.equals("Amount")) amount = it.path("Value").decimalValue();
        }
        long paid = amount == null ? 0 : amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        if (receipt == null || receipt.isBlank() || paid < b.getTotalCents()) {
            b.setNote(clip("Check the M-Pesa statement: receipt " + receipt + ", amount " + (amount == null ? "?" : amount)));
            bills.save(b);
            log.error("Commission bill {} callback needs a manual check (receipt {}, amount {})", b.getId(), receipt, amount);
            return true;
        }
        b.setStatus(CommissionBill.PAID);
        b.setPaidVia("MPESA");
        b.setMpesaReceipt(receipt.trim());
        b.setPaidAt(Instant.now());
        bills.save(b);
        try {
            notifier.notifyAndSms(b.getFarmer(), "CASH", "Commission received", "Receipt " + receipt + ". Thank you!", b.getId());
        } catch (Exception e) { log.warn("Could not notify farmer about bill {}: {}", b.getId(), e.toString()); }
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private void notifyAdmins(String title, String body, Long ref) {
        try {
            for (User a : users.findByRoleIn(List.of(Role.ADMIN))) notifier.notify(a, "CASH", title, body, ref);
        } catch (Exception e) { log.warn("Could not notify admins: {}", e.toString()); }
    }

    private CustomerOrder order(Long id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private CashSale sale(Long id) {
        return sales.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cash payment not found"));
    }

    private static Set<Long> ownerIds(CustomerOrder o) {
        Set<Long> ids = new LinkedHashSet<>();
        for (OrderItem i : o.getItems()) ids.add(i.getProduct().getFarm().getOwner().getId());
        return ids;
    }

    private static String clip(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        return s.length() > 300 ? s.substring(0, 300) : s;
    }

    private static double kes(long cents) { return cents / 100.0; }

    private List<CashView> views(List<CashSale> list) { return list.stream().map(this::view).toList(); }

    private CashView view(CashSale c) {
        CashMath.Parts p = CashMath.split(c.getAmountCents(), policy.getPlatformBps(), policy.getCountyBps());
        CustomerOrder o = c.getOrder();
        BillView bv = bills.findByCashSaleId(c.getId()).map(this::bill).orElse(null);
        return new CashView(c.getId(), o.getId(), c.getFarmer().getId(), c.getFarmer().getName(), c.getFarmer().getPhone(),
                o.getBuyer().getId(), o.getBuyer().getName(), o.getBuyer().getPhone(), o.getStatus().name(),
                kes(c.getAmountCents()), kes(p.platformCents()), kes(p.countyCents()), kes(p.farmerCents()),
                c.getStatus(), c.getFarmerNote(), c.getBuyerNote(), c.getDecisionNote(),
                c.getRecordedAt(), c.getBuyerRespondedAt(), c.getDecidedAt(), bv);
    }

    private BillView bill(CommissionBill b) {
        boolean overdue = !CommissionBill.PAID.equals(b.getStatus()) && CashMath.overdue(b.getCreatedAt(), Instant.now(), maxOverdueDays);
        return new BillView(b.getId(), b.getCashSaleId(), b.getOrderId(), b.getFarmer().getId(), b.getFarmer().getName(),
                kes(b.getSaleCents()), kes(b.getPlatformCents()), kes(b.getCountyCents()), kes(b.getTotalCents()),
                b.getStatus(), b.getMpesaReceipt(), b.getPaidVia(), overdue, b.getCreatedAt(), b.getPaidAt());
    }
}

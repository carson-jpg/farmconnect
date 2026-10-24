package com.farmconnect.service;

import com.farmconnect.dto.BulkDtos.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.ProduceRequestRepository;
import com.farmconnect.repository.PurchaseOrderRepository;
import com.farmconnect.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bulk buying. A VERIFIED company that buys posts a produce request; VERIFIED farmers send quotations; the company
 * accepts a quotation, which issues a purchase order with the agreed price copied onto it; the farmer confirms; the
 * company records delivery and payment. The BULK_PURCHASE commission rate is recorded on each order; the money itself
 * is settled between the two parties for now. Everything maps to DTOs inside the transaction.
 */
@Service
@RequiredArgsConstructor
public class BulkService {
    private static final ZoneId KENYA = ZoneId.of("Africa/Nairobi");
    /** Orders that still count against the quantity a request asked for. */
    private static final Set<PurchaseOrderStatus> COMMITTED = EnumSet.of(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.DELIVERED);

    private final ProduceRequestRepository requests;
    private final QuotationRepository quotes;
    private final PurchaseOrderRepository orders;
    private final CompanyService companies;
    private final CommissionPolicyService policies;
    private final NotificationService notifications;

    // ------------------------------------------------------------------ company: requests

    @Transactional
    public RequestView create(User owner, RequestForm f) {
        Company c = buyer(owner);
        LocalDate today = LocalDate.now(KENYA);
        if (f.closesAt().isBefore(today)) throw bad("The quotation deadline cannot be in the past");
        if (f.neededBy().isBefore(f.closesAt())) throw bad("The produce cannot be needed before the quotation deadline");
        ProduceRequest r = new ProduceRequest();
        r.setCompany(c);
        apply(r, f);
        return view(requests.save(r));
    }

    @Transactional
    public RequestView update(User owner, Long id, RequestForm f) {
        Company c = buyer(owner);
        ProduceRequest r = ownRequest(c, id, true);
        if (r.getStatus() != ProduceRequestStatus.OPEN) throw conflict("Only an open request can be edited");
        if (quotes.countByRequestIdAndStatus(r.getId(), QuotationStatus.SUBMITTED) > 0)
            throw conflict("Farmers have already quoted. Close this request and post a new one.");
        if (f.neededBy().isBefore(f.closesAt())) throw bad("The produce cannot be needed before the quotation deadline");
        apply(r, f);
        r.setUpdatedAt(Instant.now());
        return view(requests.save(r));
    }

    @Transactional(readOnly = true)
    public List<RequestView> myRequests(User owner) {
        Company c = companies.entityOf(owner);
        return requests.findByCompanyIdOrderByCreatedAtDesc(c.getId()).stream().map(this::view).toList();
    }

    /** Stops the request. Quotations still waiting are declined and the farmers told. */
    @Transactional
    public RequestView close(User owner, Long id) {
        Company c = companies.entityOf(owner);
        ProduceRequest r = ownRequest(c, id, true);
        if (r.getStatus() != ProduceRequestStatus.OPEN && r.getStatus() != ProduceRequestStatus.FULFILLED)
            throw conflict("This request is already " + r.getStatus());
        r.setStatus(r.getStatus() == ProduceRequestStatus.FULFILLED ? ProduceRequestStatus.FULFILLED : ProduceRequestStatus.CLOSED);
        r.setUpdatedAt(Instant.now());
        for (Quotation q : quotes.findByRequestIdOrderByPricePerUnitAscCreatedAtAsc(r.getId())) {
            if (q.getStatus() != QuotationStatus.SUBMITTED) continue;
            q.setStatus(QuotationStatus.REJECTED);
            q.setRejectionReason("The company closed this request");
            q.setDecidedAt(Instant.now());
            quotes.save(q);
            notifications.notify(q.getFarmer(), "BULK", "Request closed", c.getName() + " closed the request for " + r.getProduce() + ".", r.getId());
        }
        if (r.getStatus() == ProduceRequestStatus.OPEN) r.setStatus(ProduceRequestStatus.CLOSED);
        return view(requests.save(r));
    }

    @Transactional(readOnly = true)
    public List<QuoteView> quotationsFor(User owner, Long requestId) {
        Company c = companies.entityOf(owner);
        ProduceRequest r = ownRequest(c, requestId, false);
        return quotes.findByRequestIdOrderByPricePerUnitAscCreatedAtAsc(r.getId()).stream().map(BulkService::view).toList();
    }

    // ------------------------------------------------------------------ company: decide on quotations

    @Transactional
    public PurchaseOrderView accept(User owner, Long quotationId) {
        Company c = buyer(owner);
        Quotation q = quotes.lockById(quotationId).orElseThrow(() -> notFound("Quotation"));
        ProduceRequest r = requests.lockById(q.getRequest().getId()).orElseThrow(() -> notFound("Request"));
        if (!r.getCompany().getId().equals(c.getId())) throw notFound("Quotation");
        if (q.getStatus() != QuotationStatus.SUBMITTED) throw conflict("This quotation is already " + q.getStatus());
        if (r.getStatus() != ProduceRequestStatus.OPEN) throw conflict("This request is " + r.getStatus());
        BigDecimal remaining = remaining(r);
        if (q.getQuantity().compareTo(remaining) > 0)
            throw bad("Only " + remaining.stripTrailingZeros().toPlainString() + " " + r.getUnit() + " of this request is still unfilled");

        BigDecimal total = q.getQuantity().multiply(q.getPricePerUnit()).setScale(2, RoundingMode.HALF_UP);
        CommissionPolicy pol = policies.current(CommissionType.BULK_PURCHASE);
        SupplyMath.Split s = SupplyMath.split(total, pol.getPlatformBps(), pol.getCountyBps());

        PurchaseOrder po = new PurchaseOrder();
        po.setRequest(r);
        po.setQuotation(q);
        po.setCompany(c);
        po.setFarmer(q.getFarmer());
        po.setProduce(r.getProduce());
        po.setQuantity(q.getQuantity());
        po.setUnit(r.getUnit());
        po.setPricePerUnit(q.getPricePerUnit());
        po.setTotal(total);
        po.setDeliveryCounty(r.getDeliveryCounty());
        po.setDeliveryLocation(r.getDeliveryLocation());
        po.setNeededBy(r.getNeededBy());
        po.setCommissionPolicyId(pol.getId());
        po.setPlatformBps(pol.getPlatformBps());
        po.setCountyBps(pol.getCountyBps());
        po.setPlatformFee(s.platform());
        po.setCountyFee(s.county());
        po.setFarmerNet(s.company());                     // the seller keeps the remainder
        po = orders.save(po);

        q.setStatus(QuotationStatus.ACCEPTED);
        q.setDecidedAt(Instant.now());
        quotes.save(q);
        if (remaining.subtract(q.getQuantity()).signum() <= 0) r.setStatus(ProduceRequestStatus.FULFILLED);
        r.setUpdatedAt(Instant.now());
        requests.save(r);
        notifications.notify(q.getFarmer(), "BULK", "Your quotation was accepted",
                c.getName() + " accepted your quotation for " + q.getQuantity().stripTrailingZeros().toPlainString() + " " + r.getUnit()
                        + " of " + r.getProduce() + ". Confirm the purchase order.", po.getId());
        return view(po, true);
    }

    @Transactional
    public QuoteView reject(User owner, Long quotationId, String reason) {
        Company c = companies.entityOf(owner);
        Quotation q = quotes.lockById(quotationId).orElseThrow(() -> notFound("Quotation"));
        if (!q.getRequest().getCompany().getId().equals(c.getId())) throw notFound("Quotation");
        if (q.getStatus() != QuotationStatus.SUBMITTED) throw conflict("This quotation is already " + q.getStatus());
        q.setStatus(QuotationStatus.REJECTED);
        q.setRejectionReason(reason == null || reason.isBlank() ? null : reason.trim());
        q.setDecidedAt(Instant.now());
        notifications.notify(q.getFarmer(), "BULK", "Quotation not accepted",
                c.getName() + " did not accept your quotation for " + q.getRequest().getProduce() + (q.getRejectionReason() == null ? "." : ". Reason: " + q.getRejectionReason()), q.getRequest().getId());
        return view(quotes.save(q));
    }

    // ------------------------------------------------------------------ farmer: browse and quote

    @Transactional(readOnly = true)
    public List<RequestView> browse(String produce, String county) {
        String p = produce == null ? "" : produce.trim().toLowerCase(Locale.ROOT);
        String k = county == null ? "" : county.trim().toLowerCase(Locale.ROOT);
        return requests.openFor(LocalDate.now(KENYA)).stream()
                .filter(r -> p.isEmpty() || r.getProduce().toLowerCase(Locale.ROOT).contains(p))
                .filter(r -> k.isEmpty() || (r.getDeliveryCounty() != null && r.getDeliveryCounty().toLowerCase(Locale.ROOT).equals(k)))
                .map(this::view).filter(v -> v.remaining().signum() > 0).toList();
    }

    @Transactional
    public QuoteView quote(User farmer, Long requestId, QuoteForm f) {
        if (!farmer.isVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only verified farmers can send quotations. Complete your farmer verification first.");
        ProduceRequest r = requests.lockById(requestId).orElseThrow(() -> notFound("Request"));
        if (r.getStatus() != ProduceRequestStatus.OPEN || r.getCompany().getStatus() != CompanyStatus.VERIFIED || r.getClosesAt().isBefore(LocalDate.now(KENYA)))
            throw conflict("This request is no longer taking quotations");
        if (quotes.existsByRequestIdAndFarmerIdAndStatusIn(r.getId(), farmer.getId(), EnumSet.of(QuotationStatus.SUBMITTED, QuotationStatus.ACCEPTED)))
            throw conflict("You already have a quotation on this request. Withdraw it to send a new one.");
        BigDecimal remaining = remaining(r);
        if (f.quantity().compareTo(remaining) > 0) throw bad("The company only still needs " + remaining.stripTrailingZeros().toPlainString() + " " + r.getUnit());
        if (r.getMaxPrice() != null && f.pricePerUnit().compareTo(r.getMaxPrice()) > 0)
            throw bad("The company will pay at most KSh " + r.getMaxPrice().stripTrailingZeros().toPlainString() + " per " + r.getUnit());
        Quotation q = new Quotation();
        q.setRequest(r);
        q.setFarmer(farmer);
        q.setQuantity(f.quantity());
        q.setPricePerUnit(f.pricePerUnit());
        q.setAvailableFrom(f.availableFrom());
        q.setMessage(f.message() == null || f.message().isBlank() ? null : f.message().trim());
        q = quotes.save(q);
        notifications.notify(r.getCompany().getOwner(), "BULK", "New quotation",
                farmer.getName() + " quoted KSh " + f.pricePerUnit().stripTrailingZeros().toPlainString() + " per " + r.getUnit() + " for " + r.getProduce() + ".", r.getId());
        return view(q);
    }

    @Transactional(readOnly = true)
    public List<QuoteView> myQuotations(User farmer) {
        return quotes.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream().map(BulkService::view).toList();
    }

    @Transactional
    public QuoteView withdraw(User farmer, Long id) {
        Quotation q = quotes.lockById(id).filter(x -> x.getFarmer().getId().equals(farmer.getId())).orElseThrow(() -> notFound("Quotation"));
        if (q.getStatus() != QuotationStatus.SUBMITTED) throw conflict("Only a quotation that is still waiting can be withdrawn");
        q.setStatus(QuotationStatus.WITHDRAWN);
        q.setDecidedAt(Instant.now());
        return view(quotes.save(q));
    }

    // ------------------------------------------------------------------ purchase orders

    @Transactional(readOnly = true)
    public List<PurchaseOrderView> companyOrders(User owner) {
        Company c = companies.entityOf(owner);
        return orders.findByCompanyIdOrderByCreatedAtDesc(c.getId()).stream().map(o -> view(o, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderView> farmerOrders(User farmer) {
        return orders.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream().map(o -> view(o, false)).toList();
    }

    @Transactional
    public PurchaseOrderView confirm(User farmer, Long id) {
        PurchaseOrder o = orders.lockById(id).filter(x -> x.getFarmer().getId().equals(farmer.getId())).orElseThrow(() -> notFound("Purchase order"));
        step(o, PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.CONFIRMED);
        o.setConfirmedAt(Instant.now());
        notifications.notify(o.getCompany().getOwner(), "BULK", "Purchase order #" + o.getId() + " confirmed", o.getFarmer().getName() + " confirmed the purchase order.", o.getId());
        return view(orders.save(o), false);
    }

    @Transactional
    public PurchaseOrderView decline(User farmer, Long id, String reason) {
        PurchaseOrder o = orders.lockById(id).filter(x -> x.getFarmer().getId().equals(farmer.getId())).orElseThrow(() -> notFound("Purchase order"));
        step(o, PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.DECLINED);
        o.setCancelReason(reason == null || reason.isBlank() ? "Declined by the farmer" : reason.trim());
        reopen(o);
        notifications.notify(o.getCompany().getOwner(), "BULK", "Purchase order #" + o.getId() + " declined", o.getFarmer().getName() + " declined. " + o.getCancelReason(), o.getId());
        return view(orders.save(o), false);
    }

    @Transactional
    public PurchaseOrderView deliver(User owner, Long id) {
        PurchaseOrder o = ownOrder(owner, id);
        step(o, PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.DELIVERED);
        o.setDeliveredAt(Instant.now());
        notifications.notify(o.getFarmer(), "BULK", "Delivery recorded", o.getCompany().getName() + " recorded delivery for purchase order #" + o.getId() + ".", o.getId());
        return view(orders.save(o), true);
    }

    @Transactional
    public PurchaseOrderView markPaid(User owner, Long id) {
        PurchaseOrder o = ownOrder(owner, id);
        if (o.getStatus() != PurchaseOrderStatus.DELIVERED) throw conflict("Only a delivered order can be marked paid");
        if ("PAID".equals(o.getPaymentStatus())) throw conflict("Already marked paid");
        o.setPaymentStatus("PAID");
        o.setPaidAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        notifications.notify(o.getFarmer(), "BULK", "Purchase order #" + o.getId() + " paid", o.getCompany().getName() + " recorded your payment.", o.getId());
        return view(orders.save(o), true);
    }

    @Transactional
    public PurchaseOrderView cancel(User owner, Long id, String reason) {
        PurchaseOrder o = ownOrder(owner, id);
        if (o.getStatus() != PurchaseOrderStatus.ISSUED && o.getStatus() != PurchaseOrderStatus.CONFIRMED)
            throw conflict("This order is already " + o.getStatus());
        o.setStatus(PurchaseOrderStatus.CANCELLED);
        o.setCancelReason(reason.trim());
        o.setUpdatedAt(Instant.now());
        reopen(o);
        notifications.notify(o.getFarmer(), "BULK", "Purchase order #" + o.getId() + " cancelled", "Reason: " + reason.trim(), o.getId());
        return view(orders.save(o), true);
    }

    // ------------------------------------------------------------------ super admin

    @Transactional(readOnly = true)
    public List<RequestView> adminRequests(ProduceRequestStatus status) {
        return (status == null ? requests.findTop200ByOrderByCreatedAtDesc() : requests.findTop200ByStatusOrderByCreatedAtDesc(status))
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderView> adminOrders(PurchaseOrderStatus status) {
        return (status == null ? orders.findTop200ByOrderByCreatedAtDesc() : orders.findTop200ByStatusOrderByCreatedAtDesc(status))
                .stream().map(o -> view(o, true)).toList();
    }

    // ------------------------------------------------------------------ helpers

    /** A company that may BUY: VERIFIED and set up to buy produce. */
    private Company buyer(User owner) {
        Company c = companies.requireVerified(owner);
        if (!c.isCanBuy()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is not set up to buy produce");
        return c;
    }

    private ProduceRequest ownRequest(Company c, Long id, boolean lock) {
        var r = lock ? requests.lockById(id) : requests.findById(id);
        return r.filter(x -> x.getCompany().getId().equals(c.getId())).orElseThrow(() -> notFound("Request"));
    }

    private PurchaseOrder ownOrder(User owner, Long id) {
        Company c = companies.entityOf(owner);
        return orders.lockById(id).filter(x -> x.getCompany().getId().equals(c.getId())).orElseThrow(() -> notFound("Purchase order"));
    }

    private static void step(PurchaseOrder o, PurchaseOrderStatus from, PurchaseOrderStatus to) {
        if (o.getStatus() != from) throw conflict("This order is " + o.getStatus() + ", not " + from);
        o.setStatus(to);
        o.setUpdatedAt(Instant.now());
    }

    /** An order that falls through gives its quantity back to the request, which can take quotations again if it is still before its deadline. */
    private void reopen(PurchaseOrder o) {
        ProduceRequest r = requests.lockById(o.getRequest().getId()).orElse(null);
        if (r == null) return;
        if (r.getStatus() == ProduceRequestStatus.FULFILLED && !r.getClosesAt().isBefore(LocalDate.now(KENYA))) {
            r.setStatus(ProduceRequestStatus.OPEN);
            r.setUpdatedAt(Instant.now());
            requests.save(r);
        }
        Quotation q = o.getQuotation();
        if (q.getStatus() == QuotationStatus.ACCEPTED) {
            q.setStatus(QuotationStatus.WITHDRAWN);
            quotes.save(q);
        }
    }

    private BigDecimal remaining(ProduceRequest r) {
        BigDecimal used = orders.committedQuantity(r.getId(), COMMITTED);
        return r.getQuantity().subtract(used == null ? BigDecimal.ZERO : used).max(BigDecimal.ZERO);
    }

    private static void apply(ProduceRequest r, RequestForm f) {
        r.setProduce(f.produce().trim());
        r.setDescription(f.description() == null || f.description().isBlank() ? null : f.description().trim());
        r.setQuantity(f.quantity());
        r.setUnit(f.unit().trim());
        r.setMaxPrice(f.maxPrice());
        r.setDeliveryCounty(f.deliveryCounty().trim());
        r.setDeliveryLocation(f.deliveryLocation().trim());
        r.setNeededBy(f.neededBy());
        r.setClosesAt(f.closesAt());
    }

    private RequestView view(ProduceRequest r) {
        return new RequestView(r.getId(), r.getCompany().getId(), r.getCompany().getName(), r.getCompany().getCounty(), r.getProduce(),
                r.getDescription(), r.getQuantity(), r.getUnit(), r.getMaxPrice(), r.getDeliveryCounty(), r.getDeliveryLocation(),
                r.getNeededBy(), r.getClosesAt(), r.getStatus(), remaining(r),
                quotes.countByRequestIdAndStatus(r.getId(), QuotationStatus.SUBMITTED), r.getCreatedAt());
    }

    static QuoteView view(Quotation q) {
        BigDecimal total = q.getQuantity().multiply(q.getPricePerUnit()).setScale(2, RoundingMode.HALF_UP);
        return new QuoteView(q.getId(), q.getRequest().getId(), q.getRequest().getProduce(), q.getRequest().getUnit(),
                q.getRequest().getCompany().getName(), q.getFarmer().getName(), q.getFarmer().getPhone(), q.getQuantity(),
                q.getPricePerUnit(), total, q.getAvailableFrom(), q.getMessage(), q.getStatus(), q.getRejectionReason(), q.getCreatedAt());
    }

    static PurchaseOrderView view(PurchaseOrder o, boolean forCompany) {
        String name = forCompany ? o.getFarmer().getName() : o.getCompany().getRepName();
        String phone = forCompany ? o.getFarmer().getPhone() : o.getCompany().getRepPhone();
        return new PurchaseOrderView(o.getId(), o.getRequest().getId(), o.getProduce(), o.getUnit(), o.getCompany().getName(),
                o.getFarmer().getName(), name, phone, o.getQuantity(), o.getPricePerUnit(), o.getTotal(), o.getPlatformFee(), o.getCountyFee(),
                o.getFarmerNet(), o.getDeliveryCounty(), o.getDeliveryLocation(), o.getNeededBy(), o.getStatus(), o.getPaymentStatus(),
                o.getCancelReason(), o.getCreatedAt(), o.getDeliveredAt(), o.getPaidAt());
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }
    private static ResponseStatusException notFound(String what) { return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found"); }
}

package com.farmconnect.service;

import com.farmconnect.dto.SupplyDtos.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.CompanyProductRepository;
import com.farmconnect.repository.SupplyOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Supplier listings and farmer orders for inputs. Only VERIFIED companies that sell can list; only their active
 * listings are visible to farmers. Stock is locked and taken when the order is placed and given back if it is
 * cancelled or rejected. Everything maps to DTOs inside the transaction (open-in-view is off in production).
 */
@Service
@RequiredArgsConstructor
public class SupplyService {
    private final CompanyProductRepository products;
    private final SupplyOrderRepository orders;
    private final CompanyService companies;
    private final CommissionPolicyService policies;
    private final NotificationService notifications;

    // ------------------------------------------------------------------ company: listings

    @Transactional(readOnly = true)
    public List<ProductView> myProducts(User owner) {
        Company c = seller(owner);
        return products.findByCompanyIdOrderByNameAsc(c.getId()).stream().map(SupplyService::view).toList();
    }

    @Transactional
    public ProductView createProduct(User owner, ProductRequest r) {
        Company c = seller(owner);
        CompanyProduct p = new CompanyProduct();
        p.setCompany(c);
        apply(p, r);
        return view(products.save(p));
    }

    @Transactional
    public ProductView updateProduct(User owner, Long id, ProductRequest r) {
        Company c = seller(owner);
        CompanyProduct p = ownProduct(c, id);
        apply(p, r);
        p.setUpdatedAt(Instant.now());
        return view(products.save(p));
    }

    /** A listing is never deleted (old orders point at it); it is switched off. */
    @Transactional
    public ProductView deactivate(User owner, Long id) {
        Company c = seller(owner);
        CompanyProduct p = ownProduct(c, id);
        p.setActive(false);
        p.setUpdatedAt(Instant.now());
        return view(products.save(p));
    }

    // ------------------------------------------------------------------ farmer: browse and order

    @Transactional(readOnly = true)
    public List<ProductView> browse(SupplyCategory category, String q) {
        String like = "%" + (q == null ? "" : q.trim().replace("%", "").replace("_", "")) + "%";
        return products.browse(like).stream().filter(p -> category == null || p.getCategory() == category)
                .filter(p -> p.getStockQty() > 0).map(SupplyService::view).toList();
    }

    @Transactional
    public OrderView place(User buyer, PlaceOrderRequest r) {
        // merge repeated products, then lock in id order so two orders can never deadlock
        TreeMap<Long, Integer> wanted = new TreeMap<>();
        for (Line l : r.items()) wanted.merge(l.productId(), l.quantity(), Integer::sum);

        SupplyOrder o = new SupplyOrder();
        o.setBuyer(buyer);
        Company company = null;
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            CompanyProduct p = products.lockById(e.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A product in your order no longer exists"));
            int qty = e.getValue();
            if (!p.isActive() || p.getCompany().getStatus() != CompanyStatus.VERIFIED)
                throw bad(p.getName() + " is not available");
            if (company == null) company = p.getCompany();
            else if (!company.getId().equals(p.getCompany().getId()))
                throw bad("One order can only contain products from one company. Place separate orders.");
            if (qty < p.getMinOrderQty()) throw bad("Minimum order for " + p.getName() + " is " + p.getMinOrderQty());
            if (qty > p.getStockQty()) throw bad("Only " + p.getStockQty() + " of " + p.getName() + " left");
            p.setStockQty(p.getStockQty() - qty);
            products.save(p);

            SupplyOrderItem it = new SupplyOrderItem();
            it.setOrder(o);
            it.setProduct(p);
            it.setProductName(p.getName());
            it.setUnit(p.getUnit());
            it.setQuantity(qty);
            it.setUnitPrice(p.getPrice());
            o.getItems().add(it);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        o.setCompany(company);
        o.setTotal(total);
        CommissionPolicy pol = policies.current(CommissionType.SUPPLY_SALE);
        SupplyMath.Split s = SupplyMath.split(total, pol.getPlatformBps(), pol.getCountyBps());
        o.setCommissionPolicyId(pol.getId());
        o.setPlatformBps(pol.getPlatformBps());
        o.setCountyBps(pol.getCountyBps());
        o.setPlatformFee(s.platform());
        o.setCountyFee(s.county());
        o.setCompanyNet(s.company());
        o.setDeliveryAddress(r.deliveryAddress().trim());
        o.setDeliveryPhone(r.deliveryPhone().trim());
        o.setDeliveryNote(r.deliveryNote() == null || r.deliveryNote().isBlank() ? null : r.deliveryNote().trim());
        o = orders.save(o);
        notifications.notify(company.getOwner(), "SUPPLY", "New order #" + o.getId(),
                buyer.getName() + " ordered KSh " + total.toPlainString() + ". Confirm or reject it.", o.getId());
        return view(o, false);
    }

    @Transactional(readOnly = true)
    public List<OrderView> myOrders(User buyer) {
        return orders.findByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream().map(o -> view(o, false)).toList();
    }

    @Transactional(readOnly = true)
    public OrderView myOrder(User buyer, Long id) {
        SupplyOrder o = orders.findById(id).filter(x -> x.getBuyer().getId().equals(buyer.getId())).orElseThrow(SupplyService::notFound);
        return view(o, false);
    }

    @Transactional
    public OrderView cancel(User buyer, Long id) {
        SupplyOrder o = orders.lockById(id).filter(x -> x.getBuyer().getId().equals(buyer.getId())).orElseThrow(SupplyService::notFound);
        if (o.getStatus() != SupplyOrderStatus.PLACED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The company has already handled this order. Contact them to cancel.");
        o.setStatus(SupplyOrderStatus.CANCELLED);
        o.setUpdatedAt(Instant.now());
        restoreStock(o);
        orders.save(o);
        notifications.notify(o.getCompany().getOwner(), "SUPPLY", "Order #" + o.getId() + " cancelled", "The farmer cancelled this order.", o.getId());
        return view(o, false);
    }

    // ------------------------------------------------------------------ company: orders

    @Transactional(readOnly = true)
    public List<OrderView> companyOrders(User owner, SupplyOrderStatus status) {
        Company c = companyOf(owner);
        List<SupplyOrder> list = status == null ? orders.findByCompanyIdOrderByCreatedAtDesc(c.getId())
                : orders.findByCompanyIdAndStatusOrderByCreatedAtDesc(c.getId(), status);
        return list.stream().map(o -> view(o, true)).toList();
    }

    @Transactional(readOnly = true)
    public OrderView companyOrder(User owner, Long id) {
        return view(ownOrder(companyOf(owner), id, false), true);
    }

    @Transactional
    public OrderView confirm(User owner, Long id) {
        SupplyOrder o = ownOrder(companyOf(owner), id, true);
        move(o, SupplyOrderStatus.PLACED, SupplyOrderStatus.CONFIRMED);
        tell(o, "Order #" + o.getId() + " confirmed", o.getCompany().getName() + " confirmed your order.");
        return view(orders.save(o), true);
    }

    @Transactional
    public OrderView dispatch(User owner, Long id) {
        SupplyOrder o = ownOrder(companyOf(owner), id, true);
        move(o, SupplyOrderStatus.CONFIRMED, SupplyOrderStatus.DISPATCHED);
        tell(o, "Order #" + o.getId() + " is on its way", o.getCompany().getName() + " dispatched your order.");
        return view(orders.save(o), true);
    }

    @Transactional
    public OrderView deliver(User owner, Long id) {
        SupplyOrder o = ownOrder(companyOf(owner), id, true);
        move(o, SupplyOrderStatus.DISPATCHED, SupplyOrderStatus.DELIVERED);
        o.setDeliveredAt(Instant.now());
        tell(o, "Order #" + o.getId() + " delivered", "Your order was marked delivered. Pay the company as agreed.");
        return view(orders.save(o), true);
    }

    @Transactional
    public OrderView reject(User owner, Long id, String reason) {
        SupplyOrder o = ownOrder(companyOf(owner), id, true);
        if (o.getStatus() != SupplyOrderStatus.PLACED && o.getStatus() != SupplyOrderStatus.CONFIRMED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already " + o.getStatus());
        o.setStatus(SupplyOrderStatus.REJECTED);
        o.setRejectionReason(reason.trim());
        o.setUpdatedAt(Instant.now());
        restoreStock(o);
        tell(o, "Order #" + o.getId() + " not accepted", "Reason: " + reason.trim());
        return view(orders.save(o), true);
    }

    /** The company has received the money for a delivered order. */
    @Transactional
    public OrderView markPaid(User owner, Long id) {
        SupplyOrder o = ownOrder(companyOf(owner), id, true);
        if (o.getStatus() != SupplyOrderStatus.DELIVERED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a delivered order can be marked paid");
        if ("PAID".equals(o.getPaymentStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already marked paid");
        if ("PENDING".equals(o.getPaymentStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An M-Pesa payment is in progress for this order. Wait for it to finish.");
        o.setPaymentStatus("PAID");
        o.setPaymentMethod("CASH");
        o.setPaidAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        tell(o, "Order #" + o.getId() + " marked paid", o.getCompany().getName() + " recorded your payment. Thank you!");
        return view(orders.save(o), true);
    }

    /** Super admin: the latest 200 orders across all companies, with the commission shown. */
    @Transactional(readOnly = true)
    public List<OrderView> adminOrders(SupplyOrderStatus status) {
        return (status == null ? orders.findTop200ByOrderByCreatedAtDesc() : orders.findTop200ByStatusOrderByCreatedAtDesc(status))
                .stream().map(o -> view(o, true)).toList();
    }

    @Transactional(readOnly = true)
    public CompanySummary summary(User owner) {
        Company c = companyOf(owner);
        List<SupplyOrder> all = orders.findByCompanyIdOrderByCreatedAtDesc(c.getId());
        int fresh = 0, progress = 0, done = 0;
        BigDecimal gross = BigDecimal.ZERO, fees = BigDecimal.ZERO, net = BigDecimal.ZERO, unpaid = BigDecimal.ZERO;
        for (SupplyOrder o : all) {
            switch (o.getStatus()) {
                case PLACED -> fresh++;
                case CONFIRMED, DISPATCHED -> progress++;
                case DELIVERED -> {
                    done++;
                    gross = gross.add(o.getTotal());
                    fees = fees.add(o.getPlatformFee()).add(o.getCountyFee());
                    net = net.add(o.getCompanyNet());
                    if (!"PAID".equals(o.getPaymentStatus())) unpaid = unpaid.add(o.getCompanyNet());
                }
                default -> { }
            }
        }
        List<CompanyProduct> mine = products.findByCompanyIdOrderByNameAsc(c.getId());
        int active = (int) mine.stream().filter(CompanyProduct::isActive).count();
        int low = (int) mine.stream().filter(p -> p.isActive() && p.getStockQty() <= 5).count();
        return new CompanySummary(fresh, progress, done, active, low, gross, fees, net, unpaid);
    }

    // ------------------------------------------------------------------ helpers

    /** The caller's company, which must be VERIFIED and allowed to sell. */
    private Company seller(User owner) {
        Company c = companies.requireVerified(owner);
        if (!c.isCanSell()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is not set up to sell products");
        return c;
    }

    /** Orders can still be seen and finished after a company is suspended, so this only needs the company, not VERIFIED. */
    private Company companyOf(User owner) {
        return companies.entityOf(owner);
    }

    private CompanyProduct ownProduct(Company c, Long id) {
        return products.findById(id).filter(p -> p.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    /** lock=true takes a row lock so two taps cannot move one order twice. */
    private SupplyOrder ownOrder(Company c, Long id, boolean lock) {
        Optional<SupplyOrder> o = lock ? orders.lockById(id) : orders.findById(id);
        return o.filter(x -> x.getCompany().getId().equals(c.getId())).orElseThrow(SupplyService::notFound);
    }

    private static void move(SupplyOrder o, SupplyOrderStatus from, SupplyOrderStatus to) {
        if (o.getStatus() != from)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is " + o.getStatus() + ", not " + from);
        o.setStatus(to);
        o.setUpdatedAt(Instant.now());
    }

    private void restoreStock(SupplyOrder o) {
        for (SupplyOrderItem i : o.getItems()) {
            products.lockById(i.getProduct().getId()).ifPresent(p -> {
                p.setStockQty(p.getStockQty() + i.getQuantity());
                products.save(p);
            });
        }
    }

    private void tell(SupplyOrder o, String title, String body) {
        notifications.notify(o.getBuyer(), "SUPPLY", title, body, o.getId());
    }

    private void apply(CompanyProduct p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setCategory(r.category());
        p.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        p.setUnit(r.unit().trim());
        p.setPrice(r.price().setScale(2, java.math.RoundingMode.HALF_UP));
        p.setStockQty(r.stockQty());
        p.setMinOrderQty(r.minOrderQty());
        p.setLicenceRef(r.licenceRef() == null || r.licenceRef().isBlank() ? null : r.licenceRef().trim());
        p.setActive(r.active());
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"); }

    static ProductView view(CompanyProduct p) {
        return new ProductView(p.getId(), p.getCompany().getId(), p.getCompany().getName(), p.getCompany().getCounty(), p.getName(),
                p.getCategory(), p.getDescription(), p.getUnit(), p.getPrice(), p.getStockQty(), p.getMinOrderQty(),
                p.getLicenceRef(), p.isActive());
    }

    /** forCompany=false hides the commission: the farmer only sees what they pay. */
    static OrderView view(SupplyOrder o, boolean forCompany) {
        List<ItemView> items = o.getItems().stream().map(i -> new ItemView(i.getProduct().getId(), i.getProductName(), i.getUnit(),
                i.getQuantity(), i.getUnitPrice(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();
        return new OrderView(o.getId(), o.getStatus(), o.getPaymentStatus(), o.getCompany().getId(), o.getCompany().getName(),
                o.getBuyer().getName(), o.getDeliveryPhone(), o.getDeliveryAddress(), o.getDeliveryNote(), items, o.getTotal(),
                forCompany ? o.getPlatformFee() : null, forCompany ? o.getCountyFee() : null, forCompany ? o.getCompanyNet() : null,
                o.getRejectionReason(), o.getCreatedAt(), o.getDeliveredAt(), o.getPaidAt(), o.getPaymentMethod(), o.getMpesaReceipt());
    }
}

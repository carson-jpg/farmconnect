package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orders;
    private final CartItemRepository cart;
    private final com.farmconnect.service.NotificationService notifier;

    /** Buyer: place an order from the current cart. */
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody OrderRequest r, @AuthenticationPrincipal User u) {
        List<CartItem> items = cart.findByUserId(u.getId());
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");

        CustomerOrder o = new CustomerOrder();
        o.setBuyer(u);
        o.setStatus(OrderStatus.PENDING);
        o.setDeliveryAddress(r.deliveryAddress().trim());
        o.setDeliveryPhone(r.deliveryPhone() == null ? null : r.deliveryPhone().trim());
        o.setDeliveryNote(r.deliveryNote() == null || r.deliveryNote().isBlank() ? null : r.deliveryNote().trim());
        String pay = r.paymentMethod() == null ? "CASH_ON_DELIVERY" : r.paymentMethod().trim().toUpperCase();
        if (!pay.equals("CASH_ON_DELIVERY") && !pay.equals("MPESA_ON_DELIVERY"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown payment method");
        o.setPaymentMethod(pay);
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(o.getCreatedAt());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : items) {
            Product p = ci.getProduct();
            if (!p.isActive() || !p.getFarm().getOwner().isVerified())
                throw new ResponseStatusException(HttpStatus.CONFLICT, p.getName() + " is no longer available. Remove it from your cart");
            if (p.getQuantity() < ci.getQuantity())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + p.getName());
            p.setQuantity(p.getQuantity() - ci.getQuantity());
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setProduct(p);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(p.getPrice());
            o.getItems().add(oi);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        o.setTotal(total);
        orders.save(o);
        cart.deleteByUserId(u.getId());
        notifyFarmers(o, "New order #" + o.getId(),
                u.getName() + " ordered " + o.getItems().size() + (o.getItems().size() == 1 ? " product" : " products")
                        + " (KES " + total.toBigInteger() + "). Open Orders to confirm.");
        return Mapper.order(o);
    }

    /** Buyer: my orders. */
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public List<OrderResponse> mine(@AuthenticationPrincipal User u) {
        return orders.findByBuyerIdOrderByCreatedAtDesc(u.getId()).stream().map(Mapper::order).toList();
    }

    /** Buyer: cancel a PENDING order (stock is returned). */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
    public OrderResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User u) {
        CustomerOrder o = find(id);
        if (!o.getBuyer().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        if (o.getStatus() != OrderStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending orders can be cancelled");
        o.getItems().forEach(i -> i.getProduct().setQuantity(i.getProduct().getQuantity() + i.getQuantity()));
        o.setStatus(OrderStatus.CANCELLED);
        o.setUpdatedAt(Instant.now());
        CustomerOrder saved = orders.save(o);
        notifyFarmers(saved, "Order #" + saved.getId() + " cancelled", saved.getBuyer().getName() + " cancelled this order. The stock was returned.");
        return Mapper.order(saved);
    }

    /** Farmer: orders containing products from my farms. */
    @GetMapping("/farmer")
    @PreAuthorize("hasRole('FARMER')")
    public List<OrderResponse> forFarmer(@AuthenticationPrincipal User u) {
        return orders.findForFarmer(u.getId()).stream().map(Mapper::order).toList();
    }

    /** Farmer: update status of an order that contains my products. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r,
                                      @AuthenticationPrincipal User u) {
        CustomerOrder o = find(id);
        boolean mine = o.getItems().stream().anyMatch(i -> i.getProduct().getFarm().getOwner().getId().equals(u.getId()));
        if (!mine) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        if (r.status() == OrderStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use buyer cancel endpoint");
        o.setStatus(r.status());
        o.setUpdatedAt(Instant.now());
        CustomerOrder saved = orders.save(o);
        notifier.notifyAndSms(saved.getBuyer(), "ORDER", "Order #" + saved.getId() + " " + statusText(r.status()),
                "Your order is now " + r.status().name().toLowerCase() + ".", saved.getId());
        return Mapper.order(saved);
    }

    private CustomerOrder find(Long id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void notifyFarmers(CustomerOrder o, String title, String body) {
        java.util.Map<Long, User> owners = new java.util.LinkedHashMap<>();
        for (OrderItem i : o.getItems()) {
            User f = i.getProduct().getFarm().getOwner();
            owners.putIfAbsent(f.getId(), f);
        }
        owners.values().forEach(f -> notifier.notifyAndSms(f, "ORDER", title, body, o.getId()));
    }

    static String statusText(OrderStatus s) {
        return switch (s) {
            case CONFIRMED -> "confirmed by the seller";
            case SHIPPED -> "is on the way";
            case DELIVERED -> "delivered";
            case CANCELLED -> "cancelled";
            default -> "updated";
        };
    }
}

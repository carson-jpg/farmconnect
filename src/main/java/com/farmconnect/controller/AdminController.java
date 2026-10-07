package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Platform administration: statistics, users, every order, product moderation. ADMIN only. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final FarmerVerificationRepository verifications;
    private final com.farmconnect.service.NotificationService notifier;

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public AdminStats stats() {
        List<User> all = users.findAll();
        List<Product> prods = products.findAll();
        List<CustomerOrder> ords = orders.findAll();
        BigDecimal revenue = ords.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminStats(
                all.size(),
                all.stream().filter(u -> u.getRole() == Role.FARMER).count(),
                all.stream().filter(u -> u.getRole() == Role.FARMER && u.isVerified()).count(),
                all.stream().filter(u -> u.getRole() == Role.BUYER).count(),
                all.stream().filter(u -> u.getRole() == Role.OFFICER).count(),
                verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.PENDING).size(),
                verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.UNDER_REVIEW).size(),
                verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.REJECTED).size(),
                prods.size(),
                prods.stream().filter(p -> p.isActive() && p.getFarm().getOwner().isVerified()).count(),
                ords.size(),
                ords.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count(),
                ords.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count(),
                ords.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count(),
                revenue);
    }

    // ---------------- users ----------------

    @GetMapping("/users")
    public List<UserSummary> listUsers(@RequestParam(required = false) Role role,
                                       @RequestParam(required = false) String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return users.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> needle.isEmpty()
                        || (u.getName() != null && u.getName().toLowerCase().contains(needle))
                        || u.getEmail().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(User::getId).reversed())
                .map(Mapper::user).toList();
    }

    @PostMapping("/users/{id}/enabled")
    @Transactional
    public UserSummary setEnabled(@PathVariable Long id, @RequestBody ActiveRequest r, @AuthenticationPrincipal User admin) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getId().equals(admin.getId()) || u.getRole() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrator accounts cannot be suspended");
        u.setEnabled(r.active());
        return Mapper.user(users.save(u));
    }

    // ---------------- orders ----------------

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public List<OrderResponse> allOrders(@RequestParam(required = false) OrderStatus status) {
        return orders.findAll().stream()
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(Comparator.comparing(CustomerOrder::getCreatedAt).reversed())
                .map(Mapper::order).toList();
    }

    /** Admin override: move an order to any status. Cancelling returns the stock. */
    @PatchMapping("/orders/{id}/status")
    @Transactional
    public OrderResponse setOrderStatus(@PathVariable Long id, @RequestBody StatusRequest r) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (o.getStatus() == r.status()) return Mapper.order(o);
        if (o.getStatus() == OrderStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cancelled order cannot be reopened");
        if (r.status() == OrderStatus.CANCELLED) {
            if (o.getStatus() == OrderStatus.DELIVERED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A delivered order cannot be cancelled");
            o.getItems().forEach(i -> i.getProduct().setQuantity(i.getProduct().getQuantity() + i.getQuantity()));
        }
        o.setStatus(r.status());
        o.setUpdatedAt(Instant.now());
        CustomerOrder saved = orders.save(o);
        notifier.notifyAndSms(saved.getBuyer(), "ORDER", "Order #" + saved.getId() + " " + OrderController.statusText(r.status()),
                "Your order is now " + r.status().name().toLowerCase() + ".", saved.getId());
        return Mapper.order(saved);
    }

    // ---------------- products (moderation) ----------------

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<ProductResponse> allProducts(@RequestParam(required = false) String q) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return products.findAll().stream()
                .filter(p -> needle.isEmpty() || p.getName().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Product::getId).reversed())
                .map(Mapper::product).toList();
    }

    @PostMapping("/products/{id}/active")
    @Transactional
    public ProductResponse setProductActive(@PathVariable Long id, @RequestBody ActiveRequest r) {
        Product p = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        p.setActive(r.active());
        return Mapper.product(products.save(p));
    }
}

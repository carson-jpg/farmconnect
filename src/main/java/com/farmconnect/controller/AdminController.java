package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.security.Staff;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.AreaService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Administration: statistics, users, orders, product moderation.
 * The super admin (ADMIN) works on the whole platform. A county admin (COUNTY_ADMIN) gets the same tools, but every
 * list, number and action is limited to farmers, orders and products inside their one county.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final FarmerVerificationRepository verifications;
    private final com.farmconnect.service.NotificationService notifier;
    private final AreaService areas;

    // ---------------- who is inside my area ----------------

    /** Farmers in the admin's area, or null when the admin sees everything. */
    private Set<Long> farmerIds(User me) { return areas.farmerIdsIn(AreaScope.of(me)); }

    /** Orders that include at least one item from the admin's area. */
    private List<CustomerOrder> ordersFor(User me) {
        Set<Long> ids = farmerIds(me);
        return orders.findAll().stream().filter(o -> AreaService.orderTouches(o, ids)).toList();
    }

    /**
     * Which users an admin may see: the super admin everyone; a county admin the farmers in their county, the buyers
     * who ordered from those farmers, and the officers assigned to the county. Never admins or other counties.
     */
    private Predicate<User> userFilter(User me) {
        if (!Staff.isCountyAdmin(me)) return u -> true;
        AreaScope sc = AreaScope.of(me);
        Set<Long> farmerIds = farmerIds(me);
        Set<Long> buyerIds = new HashSet<>();
        for (CustomerOrder o : orders.findAll()) if (AreaService.orderTouches(o, farmerIds)) buyerIds.add(o.getBuyer().getId());
        return u -> switch (u.getRole()) {
            case FARMER -> farmerIds.contains(u.getId());
            case BUYER -> buyerIds.contains(u.getId());
            case OFFICER -> sc.allowsCounty(u.getAssignedCounty());
            default -> false;
        };
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public AdminStats stats(@AuthenticationPrincipal User me) {
        AreaScope sc = AreaScope.of(me);
        Set<Long> ids = farmerIds(me);
        Predicate<User> visible = userFilter(me);
        List<User> all = users.findAll().stream().filter(visible).toList();
        List<Product> prods = products.findAll().stream()
                .filter(p -> ids == null || ids.contains(p.getFarm().getOwner().getId())).toList();
        List<CustomerOrder> ords = ordersFor(me);
        BigDecimal revenue = ords.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(o -> AreaService.areaTotal(o, ids)).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FarmerVerification> pending = verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.PENDING).stream()
                .filter(v -> sc.allows(v.getSubCounty())).toList();
        List<FarmerVerification> review = verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.UNDER_REVIEW).stream()
                .filter(v -> sc.allows(v.getSubCounty())).toList();
        List<FarmerVerification> rejected = verifications.findByStatusOrderBySubmittedAtAsc(VerificationStatus.REJECTED).stream()
                .filter(v -> sc.allows(v.getSubCounty())).toList();
        return new AdminStats(
                all.size(),
                all.stream().filter(u -> u.getRole() == Role.FARMER).count(),
                all.stream().filter(u -> u.getRole() == Role.FARMER && u.isVerified()).count(),
                all.stream().filter(u -> u.getRole() == Role.BUYER).count(),
                all.stream().filter(u -> u.getRole() == Role.OFFICER).count(),
                pending.size(), review.size(), rejected.size(),
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
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers(@RequestParam(required = false) Role role,
                                       @RequestParam(required = false) String q,
                                       @AuthenticationPrincipal User me) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        Predicate<User> visible = userFilter(me);
        return users.findAll().stream()
                .filter(visible)
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
        if (Staff.isCountyAdmin(admin)) {
            if (u.getRole() == Role.COUNTY_ADMIN)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the super admin can suspend a county admin");
            if (!userFilter(admin).test(u))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user is outside your county");
        }
        u.setEnabled(r.active());
        return Mapper.user(users.save(u));
    }

    // ---------------- orders ----------------

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public List<OrderResponse> allOrders(@RequestParam(required = false) OrderStatus status, @AuthenticationPrincipal User me) {
        return ordersFor(me).stream()
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(Comparator.comparing(CustomerOrder::getCreatedAt).reversed())
                .map(Mapper::order).toList();
    }

    /** Admin override: move an order to any status. Cancelling returns the stock. */
    @PatchMapping("/orders/{id}/status")
    @Transactional
    public OrderResponse setOrderStatus(@PathVariable Long id, @RequestBody StatusRequest r, @AuthenticationPrincipal User me) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        Set<Long> ids = farmerIds(me);
        if (!AreaService.orderTouches(o, ids))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        if (!AreaService.orderWithin(o, ids))      // changing it would also change another county's farmers
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This order includes farmers from another county. Ask the super admin to change it.");
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
    public List<ProductResponse> allProducts(@RequestParam(required = false) String q, @AuthenticationPrincipal User me) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        Set<Long> ids = farmerIds(me);
        return products.findAll().stream()
                .filter(p -> ids == null || ids.contains(p.getFarm().getOwner().getId()))
                .filter(p -> needle.isEmpty() || p.getName().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Product::getId).reversed())
                .map(Mapper::product).toList();
    }

    @PostMapping("/products/{id}/active")
    @Transactional
    public ProductResponse setProductActive(@PathVariable Long id, @RequestBody ActiveRequest r, @AuthenticationPrincipal User me) {
        Product p = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        Set<Long> ids = farmerIds(me);
        if (ids != null && !ids.contains(p.getFarm().getOwner().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This product is outside your county");
        p.setActive(r.active());
        return Mapper.product(products.save(p));
    }
}

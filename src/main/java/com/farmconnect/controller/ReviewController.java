package com.farmconnect.controller;

import com.farmconnect.dto.Trust.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/** Seller ratings and reviews. Only buyers who received an order can review, once per order and seller. */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final OrderRepository orders;
    private final NotificationService notifier;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody ReviewRequest r, @AuthenticationPrincipal User me) {
        CustomerOrder o = orders.findById(r.orderId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!o.getBuyer().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your order");
        if (o.getStatus() != OrderStatus.DELIVERED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can review a seller after the order is delivered");
        boolean fromSeller = o.getItems().stream().anyMatch(i -> i.getProduct().getFarm().getOwner().getId().equals(r.sellerId()));
        if (!fromSeller) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This seller was not part of the order");
        if (reviews.existsByOrderIdAndSellerId(o.getId(), r.sellerId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already reviewed this seller for this order");
        User seller = users.findById(r.sellerId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));
        Review rv = new Review();
        rv.setSeller(seller);
        rv.setBuyer(me);
        rv.setOrder(o);
        rv.setRating(r.rating());
        rv.setComment(r.comment() == null || r.comment().isBlank() ? null : r.comment().trim());
        rv = reviews.save(rv);
        recompute(seller);
        notifier.notify(seller, "REVIEW", "New " + r.rating() + "-star review",
                me.getName() + (rv.getComment() == null ? " rated you." : ": " + rv.getComment()), seller.getId());
        return toResponse(rv);
    }

    @GetMapping("/seller/{sellerId}")
    @Transactional(readOnly = true)
    public ReviewSummary forSeller(@PathVariable Long sellerId, @AuthenticationPrincipal User me) {
        User seller = users.findById(sellerId).filter(u -> u.getRole() == Role.FARMER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));
        boolean privileged = me.getId().equals(sellerId) || me.getRole() == Role.ADMIN || me.getRole() == Role.OFFICER;
        if (!seller.isVerified() && !privileged) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found");
        List<Review> list = reviews.findBySellerIdOrderByCreatedAtDesc(sellerId);
        int[] dist = new int[5];
        for (Review r : list) dist[r.getRating() - 1]++;
        return new ReviewSummary(sellerId, seller.getName(), seller.isVerified(), seller.getRatingAvg(), seller.getRatingCount(), dist,
                list.stream().map(ReviewController::toResponse).toList());
    }

    /** Delivered orders whose sellers this buyer has not rated yet. */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional(readOnly = true)
    public List<PendingReview> pending(@AuthenticationPrincipal User me) {
        List<PendingReview> out = new ArrayList<>();
        for (CustomerOrder o : orders.findAll()) {
            if (!o.getBuyer().getId().equals(me.getId()) || o.getStatus() != OrderStatus.DELIVERED) continue;
            Map<Long, List<OrderItem>> bySeller = new LinkedHashMap<>();
            for (OrderItem i : o.getItems()) bySeller.computeIfAbsent(i.getProduct().getFarm().getOwner().getId(), k -> new ArrayList<>()).add(i);
            for (var e : bySeller.entrySet()) {
                if (reviews.existsByOrderIdAndSellerId(o.getId(), e.getKey())) continue;
                OrderItem first = e.getValue().get(0);
                out.add(new PendingReview(o.getId(), e.getKey(), first.getProduct().getFarm().getName(),
                        e.getValue().stream().map(i -> i.getProduct().getName()).reduce((a, b) -> a + ", " + b).orElse(""),
                        o.getUpdatedAt() == null ? o.getCreatedAt() : o.getUpdatedAt()));
            }
        }
        return out;
    }

    /** The seller answers a review once. */
    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public ReviewResponse reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest r, @AuthenticationPrincipal User me) {
        Review rv = reviews.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        if (!rv.getSeller().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your review");
        if (rv.getReply() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "You already replied");
        rv.setReply(r.reply().trim());
        rv.setRepliedAt(Instant.now());
        notifier.notify(rv.getBuyer(), "REVIEW", me.getName() + " replied to your review", rv.getReply(), me.getId());
        return toResponse(reviews.save(rv));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable Long id) {
        Review rv = reviews.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        User seller = rv.getSeller();
        reviews.delete(rv);
        reviews.flush();
        recompute(seller);
    }

    private void recompute(User seller) {
        List<Review> list = reviews.findBySellerIdOrderByCreatedAtDesc(seller.getId());
        double avg = list.isEmpty() ? 0 : list.stream().mapToInt(Review::getRating).average().orElse(0);
        seller.setRatingAvg(Math.round(avg * 100) / 100.0);
        seller.setRatingCount(list.size());
        users.save(seller);
    }

    /** "Jane Wanjiru" -> "Jane W." so reviews are helpful without exposing full names. */
    private static String shortName(String name) {
        if (name == null || name.isBlank()) return "Buyer";
        String[] p = name.trim().split("\\s+");
        return p.length == 1 ? p[0] : p[0] + " " + Character.toUpperCase(p[p.length - 1].charAt(0)) + ".";
    }

    private static ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), shortName(r.getBuyer().getName()), r.getRating(), r.getComment(), r.getReply(),
                r.getCreatedAt(), r.getRepliedAt());
    }
}

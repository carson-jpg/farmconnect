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

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistItemRepository wishlist;
    private final ProductRepository products;

    @GetMapping
    public List<ProductResponse> list(@AuthenticationPrincipal User u) {
        return wishlist.findByUserId(u.getId()).stream().map(w -> Mapper.product(w.getProduct())).toList();
    }

    @PostMapping("/{productId}")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@PathVariable Long productId, @AuthenticationPrincipal User u) {
        if (wishlist.existsByUserIdAndProductId(u.getId(), productId)) return;
        Product p = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        WishlistItem w = new WishlistItem();
        w.setUser(u);
        w.setProduct(p);
        wishlist.save(w);
    }

    @DeleteMapping("/{productId}")
    @Transactional
    public void remove(@PathVariable Long productId, @AuthenticationPrincipal User u) {
        wishlist.deleteByUserIdAndProductId(u.getId(), productId);
    }
}

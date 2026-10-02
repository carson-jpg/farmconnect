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

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('BUYER')")
@RequiredArgsConstructor
public class CartController {
    private final CartItemRepository cart;
    private final ProductRepository products;

    @GetMapping
    public List<CartItemResponse> list(@AuthenticationPrincipal User u) {
        return cart.findByUserId(u.getId()).stream().map(Mapper::cartItem).toList();
    }

    @PostMapping
    @Transactional
    public CartItemResponse add(@Valid @RequestBody CartRequest r, @AuthenticationPrincipal User u) {
        Product p = products.findById(r.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        CartItem item = cart.findByUserIdAndProductId(u.getId(), p.getId()).orElseGet(() -> {
            CartItem c = new CartItem();
            c.setUser(u);
            c.setProduct(p);
            return c;
        });
        int qty = item.getQuantity() + r.quantity();
        if (qty > p.getQuantity())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only " + p.getQuantity() + " in stock");
        item.setQuantity(qty);
        return Mapper.cartItem(cart.save(item));
    }

    @PutMapping("/{itemId}")
    public CartItemResponse setQuantity(@PathVariable Long itemId, @Valid @RequestBody QuantityRequest r,
                                        @AuthenticationPrincipal User u) {
        CartItem item = mine(itemId, u);
        if (r.quantity() > item.getProduct().getQuantity())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only " + item.getProduct().getQuantity() + " in stock");
        item.setQuantity(r.quantity());
        return Mapper.cartItem(cart.save(item));
    }

    @DeleteMapping("/{itemId}")
    public void remove(@PathVariable Long itemId, @AuthenticationPrincipal User u) {
        cart.delete(mine(itemId, u));
    }

    @DeleteMapping
    @Transactional
    public void clear(@AuthenticationPrincipal User u) {
        cart.deleteByUserId(u.getId());
    }

    private CartItem mine(Long itemId, User u) {
        CartItem item = cart.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        if (!item.getUser().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your cart");
        return item;
    }
}

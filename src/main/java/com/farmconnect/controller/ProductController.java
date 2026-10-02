package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository products;
    private final FarmRepository farms;
    private final CartItemRepository cart;
    private final WishlistItemRepository wishlist;

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) Long farmId) {
        return products.findAll().stream()
                .filter(p -> q == null || q.isBlank() || p.getName().toLowerCase().contains(q.toLowerCase()))
                .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.getCategory()))
                .filter(p -> farmId == null || p.getFarm().getId().equals(farmId))
                .map(Mapper::product).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse one(@PathVariable Long id) {
        return Mapper.product(find(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest r, @AuthenticationPrincipal User u) {
        Product p = new Product();
        p.setFarm(ownedFarm(r.farmId(), u));
        apply(p, r);
        return Mapper.product(products.save(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest r, @AuthenticationPrincipal User u) {
        Product p = find(id);
        if (!p.getFarm().getOwner().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        p.setFarm(ownedFarm(r.farmId(), u));
        apply(p, r);
        return Mapper.product(products.save(p));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        Product p = find(id);
        if (!p.getFarm().getOwner().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        cart.deleteByProductId(id);
        wishlist.deleteByProductId(id);
        try {
            products.delete(p);
            products.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product has existing orders. Set quantity to 0 instead");
        }
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name());
        p.setDescription(r.description());
        p.setCategory(r.category());
        p.setPrice(r.price());
        p.setQuantity(r.quantity());
        p.setUnit(r.unit());
        p.setImageUrl(r.imageUrl());
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Farm ownedFarm(Long farmId, User u) {
        Farm f = farms.findById(farmId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        if (!f.getOwner().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your farm");
        return f;
    }
}

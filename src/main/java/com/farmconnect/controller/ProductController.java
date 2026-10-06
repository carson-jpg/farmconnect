package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.security.Visibility;
import com.farmconnect.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    static final int MAX_IMAGES = 6;

    private final ProductRepository products;
    private final FarmRepository farms;
    private final CartItemRepository cart;
    private final WishlistItemRepository wishlist;
    private final FileStorageService files;

    /** Public list: only products of VERIFIED farmers. A farmer also sees their own, reviewers see all. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductResponse> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) Long farmId,
                                      @AuthenticationPrincipal User viewer) {
        return products.findAll().stream()
                .filter(p -> Visibility.canSee(viewer, p.getFarm()))
                .filter(p -> q == null || q.isBlank() || p.getName().toLowerCase().contains(q.toLowerCase()))
                .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.getCategory()))
                .filter(p -> farmId == null || p.getFarm().getId().equals(farmId))
                .map(Mapper::product).toList();
    }

    /** Farmer: every product on my farms, verified or not. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional(readOnly = true)
    public List<ProductResponse> mine(@AuthenticationPrincipal User u) {
        return products.findAll().stream()
                .filter(p -> p.getFarm().getOwner().getId().equals(u.getId()))
                .map(Mapper::product).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ProductResponse one(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        Product p = find(id);
        if (!Visibility.canSee(viewer, p.getFarm()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        return Mapper.product(p);
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
    @Transactional
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest r, @AuthenticationPrincipal User u) {
        Product p = owned(id, u);
        p.setFarm(ownedFarm(r.farmId(), u));
        apply(p, r);
        return Mapper.product(products.save(p));
    }

    /** Add one photo (call several times for several photos, max 6). multipart field name: "file". */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public ProductResponse addImage(@PathVariable Long id, @RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal User u) {
        Product p = owned(id, u);
        if (p.getImages().size() >= MAX_IMAGES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A product can have at most " + MAX_IMAGES + " photos");
        p.getImages().add(files.saveProductImage(p.getId(), file));
        return Mapper.product(products.save(p));
    }

    /** Remove one photo by its position (0 = cover). */
    @DeleteMapping("/{id}/images/{index}")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public ProductResponse removeImage(@PathVariable Long id, @PathVariable int index, @AuthenticationPrincipal User u) {
        Product p = owned(id, u);
        if (index < 0 || index >= p.getImages().size())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        files.deleteProductImage(p.getImages().remove(index));
        return Mapper.product(products.save(p));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        Product p = owned(id, u);
        List<String> photos = List.copyOf(p.getImages());
        cart.deleteByProductId(id);
        wishlist.deleteByProductId(id);
        try {
            products.delete(p);
            products.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product has existing orders. Set quantity to 0 instead");
        }
        photos.forEach(files::deleteProductImage);
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setSummary(r.summary() == null ? null : r.summary().trim());
        p.setDescription(r.description() == null ? null : r.description().trim());
        p.setCategory(r.category());
        p.setPrice(r.price());
        p.setQuantity(r.quantity());
        p.setUnit(r.unit());
        if (r.imageUrl() != null) p.setImageUrl(r.imageUrl());
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Product owned(Long id, User u) {
        Product p = find(id);
        if (!p.getFarm().getOwner().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your product");
        return p;
    }

    private Farm ownedFarm(Long farmId, User u) {
        Farm f = farms.findById(farmId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        if (!f.getOwner().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your farm");
        return f;
    }
}

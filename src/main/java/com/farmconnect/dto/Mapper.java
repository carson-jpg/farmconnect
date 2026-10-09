package com.farmconnect.dto;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.model.*;

public final class Mapper {
    private Mapper() {}

    public static FarmResponse farm(Farm f) {
        return new FarmResponse(f.getId(), f.getName(), f.getLocation(), f.getDescription(), f.getOwner().getId(),
                f.getOwner().isVerified(), f.getSubCounty());
    }

    public static ProductResponse product(Product p) {
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (String path : p.getImages()) urls.add("/api/files/products/" + path);
        if (urls.isEmpty() && p.getImageUrl() != null && !p.getImageUrl().isBlank()) urls.add(p.getImageUrl());
        String cover = urls.isEmpty() ? null : urls.get(0);
        return new ProductResponse(p.getId(), p.getName(), p.getSummary(), p.getDescription(), p.getCategory(),
                p.getPrice(), p.getQuantity(), p.getUnit(), cover, urls, p.getFarm().getId(), p.getFarm().getName(),
                p.getFarm().getLocation(), p.getFarm().getOwner().isVerified(), p.isActive(), p.getFarm().getOwner().getId(),
                p.getFarm().getOwner().getRatingAvg(), p.getFarm().getOwner().getRatingCount());
    }

    public static CartItemResponse cartItem(CartItem c) {
        return new CartItemResponse(c.getId(), product(c.getProduct()), c.getQuantity());
    }

    public static OrderResponse order(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotal(), o.getDeliveryAddress(), o.getCreatedAt(),
                o.getBuyer().getName(), o.getBuyer().getPhone(),
                o.getItems().stream().map(i -> {
                    ProductResponse p = product(i.getProduct());
                    return new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(),
                            i.getPrice(), p.imageUrl(), p.farmName(), p.unit(), p.farmerId());
                }).toList(),
                o.getDeliveryPhone(), o.getDeliveryNote(), o.getPaymentMethod(),
                o.getUpdatedAt() == null ? o.getCreatedAt() : o.getUpdatedAt(), o.getBuyer().getId());
    }

    public static UserSummary user(User u) {
        return new UserSummary(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.isVerified(),
                u.isEnabled(), u.getCreatedAt());
    }

    public static VerificationResponse verification(FarmerVerification v) {
        return new VerificationResponse(v.getStatus(), v.getRejectionReason(), v.getFullName(), mask(v.getNationalId()),
                v.getPhone(), v.isPhoneVerified(), v.getCounty(), v.getSubCounty(), v.getWard(), v.getVillage(),
                v.getFarmLat(), v.getFarmLng(), v.getFarmingType(), v.getMainProduce(), v.getFarmSize(),
                v.getIdFrontPath() != null, v.getIdBackPath() != null, v.getSelfiePath() != null,
                v.getProofPath() != null, v.isTermsAccepted(), v.getSubmittedAt());
    }

    public static ReviewSummary reviewSummary(FarmerVerification v) {
        return new ReviewSummary(v.getId(), v.getFullName(), v.getSubCounty(), v.getWard(), v.getStatus(), v.getSubmittedAt());
    }

    public static ReviewDetail reviewDetail(FarmerVerification v) {
        User f = v.getFarmer();
        return new ReviewDetail(v.getId(), f.getId(), f.getName(), f.getEmail(), v.getStatus(), v.getRejectionReason(),
                v.getFullName(), v.getNationalId(), v.getPhone(), v.isPhoneVerified(), v.getCounty(), v.getSubCounty(),
                v.getWard(), v.getVillage(), v.getFarmLat(), v.getFarmLng(), v.getFarmingType(), v.getMainProduce(),
                v.getFarmSize(), v.getIdFrontPath() != null, v.getIdBackPath() != null, v.getSelfiePath() != null,
                v.getProofPath() != null, v.getSubmittedAt(), v.getReviewedAt(),
                v.getReviewedBy() == null ? null : v.getReviewedBy().getName());
    }

    private static String mask(String id) {
        if (id == null || id.length() < 4) return null;
        return "*".repeat(id.length() - 3) + id.substring(id.length() - 3);
    }
}
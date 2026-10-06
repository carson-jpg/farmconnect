package com.farmconnect.dto;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.model.*;

public final class Mapper {
    private Mapper() {}

    public static FarmResponse farm(Farm f) {
        return new FarmResponse(f.getId(), f.getName(), f.getLocation(), f.getDescription(), f.getOwner().getId(),
                f.getOwner().isVerified());
    }

    public static ProductResponse product(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getPrice(),
                p.getQuantity(), p.getUnit(), p.getImageUrl(), p.getFarm().getId(), p.getFarm().getName(),
                p.getFarm().getOwner().isVerified());
    }

    public static CartItemResponse cartItem(CartItem c) {
        return new CartItemResponse(c.getId(), product(c.getProduct()), c.getQuantity());
    }

    public static OrderResponse order(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getTotal(), o.getDeliveryAddress(), o.getCreatedAt(),
                o.getBuyer().getName(), o.getBuyer().getPhone(),
                o.getItems().stream()
                        .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(), i.getQuantity(), i.getPrice()))
                        .toList());
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
package com.farmconnect.dto;

import com.farmconnect.model.OrderStatus;
import com.farmconnect.model.Role;
import com.farmconnect.model.VerificationStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email, String phone,
                                  @NotBlank @Size(min = 6) String password, @NotNull Role role) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, Long id, String name, String email, Role role) {}

    public record FarmRequest(@NotBlank String name, String location, String description, String subCounty) {}
    public record FarmResponse(Long id, String name, String location, String description, Long ownerId,
                               boolean ownerVerified, String subCounty) {}

    public record ProductRequest(@NotBlank String name, @Size(max = 200) String summary,
                                 @Size(max = 5000) String description, String category,
                                 @NotNull @DecimalMin("0.0") BigDecimal price, @Min(0) int quantity,
                                 String unit, String imageUrl, @NotNull Long farmId) {}
    /** imageUrl = cover photo (kept for older app versions); imageUrls = every photo, cover first. */
    public record ProductResponse(Long id, String name, String summary, String description, String category,
                                  BigDecimal price, int quantity, String unit, String imageUrl,
                                  List<String> imageUrls, Long farmId, String farmName, String farmLocation,
                                  boolean farmerVerified, boolean active, Long farmerId, double sellerRating, int sellerReviews) {}

    public record CartRequest(@NotNull Long productId, @Min(1) int quantity) {}
    public record QuantityRequest(@Min(1) int quantity) {}
    public record CartItemResponse(Long id, ProductResponse product, int quantity) {}

    public record OrderRequest(@NotBlank String deliveryAddress, @Size(max = 20) String deliveryPhone,
                               @Size(max = 500) String deliveryNote, String paymentMethod) {}
    public record StatusRequest(@NotNull OrderStatus status) {}
    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal price,
                                    String imageUrl, String farmName, String unit, Long farmerId) {}
    public record OrderResponse(Long id, OrderStatus status, BigDecimal total, String deliveryAddress,
                                Instant createdAt, String buyerName, String buyerPhone,
                                List<OrderItemResponse> items, String deliveryPhone, String deliveryNote,
                                String paymentMethod, Instant updatedAt, Long buyerId) {}

    // ---- admin ----
    public record ActiveRequest(boolean active) {}
    public record UserSummary(Long id, String name, String email, String phone, Role role, boolean verified,
                              boolean enabled, Instant createdAt) {}
    public record AdminStats(long users, long farmers, long verifiedFarmers, long buyers, long officers,
                             long pendingVerifications, long underReview, long rejectedVerifications,
                             long products, long activeProducts, long orders, long pendingOrders,
                             long deliveredOrders, long cancelledOrders, BigDecimal revenue) {}

    // ---- verification ----
    public record DraftRequest(String fullName, String nationalId, String subCounty, String ward, String village,
                               Double farmLat, Double farmLng, String farmingType, String mainProduce,
                               Double farmSize, Boolean termsAccepted) {}
    public record PhoneRequest(@NotBlank String phone) {}
    public record OtpVerifyRequest(@NotBlank String phone, @NotBlank String code) {}
    public record RejectRequest(@NotBlank @Size(min = 5, max = 500) String reason) {}
    public record OfficerRequest(@NotBlank String name, @NotBlank @Email String email, String phone,
                                 @NotBlank @Size(min = 8) String password) {}

    /** What the farmer sees about their own verification. National ID is masked. */
    public record VerificationResponse(VerificationStatus status, String rejectionReason, String fullName,
                                       String maskedNationalId, String phone, boolean phoneVerified,
                                       String county, String subCounty, String ward, String village,
                                       Double farmLat, Double farmLng, String farmingType, String mainProduce,
                                       Double farmSize, boolean hasIdFront, boolean hasIdBack, boolean hasSelfie,
                                       boolean hasProof, boolean termsAccepted, Instant submittedAt) {}

    public record ReviewSummary(Long id, String fullName, String subCounty, String ward,
                                VerificationStatus status, Instant submittedAt) {}

    /** Reviewer-only view (admin / county officer). Contains the full National ID. */
    public record ReviewDetail(Long id, Long farmerId, String accountName, String accountEmail,
                               VerificationStatus status, String rejectionReason, String fullName,
                               String nationalId, String phone, boolean phoneVerified, String county,
                               String subCounty, String ward, String village, Double farmLat, Double farmLng,
                               String farmingType, String mainProduce, Double farmSize, boolean hasIdFront,
                               boolean hasIdBack, boolean hasSelfie, boolean hasProof, Instant submittedAt,
                               Instant reviewedAt, String reviewedByName) {}
}
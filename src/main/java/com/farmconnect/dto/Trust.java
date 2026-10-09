package com.farmconnect.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Market prices and seller reviews. */
public final class Trust {
    private Trust() {}

    // ---- market prices
    public record PriceRequest(@NotBlank @Size(max = 60) String crop, @NotBlank @Size(max = 60) String market,
                               @NotBlank @Size(max = 30) String unit, @NotNull @DecimalMin("0.01") BigDecimal price, LocalDate date) {}
    public record PriceResponse(Long id, String crop, String market, String unit, BigDecimal price, LocalDate date,
                                BigDecimal previousPrice, Double changePct, String postedBy, boolean subscribed) {}
    public record PricePoint(Long id, LocalDate date, BigDecimal price, String unit, String market) {}
    public record AlertToggle(String crop, boolean subscribed) {}

    // ---- reviews
    public record ReviewRequest(@NotNull Long orderId, @NotNull Long sellerId, @Min(1) @Max(5) int rating, @Size(max = 500) String comment) {}
    public record ReplyRequest(@NotBlank @Size(max = 500) String reply) {}
    public record ReviewResponse(Long id, String buyerName, int rating, String comment, String reply, Instant createdAt, Instant repliedAt) {}
    public record ReviewSummary(Long sellerId, String sellerName, boolean verified, double average, int count,
                                int[] distribution, List<ReviewResponse> reviews) {}
    public record PendingReview(Long orderId, Long sellerId, String farmName, String items, Instant deliveredAt) {}
}

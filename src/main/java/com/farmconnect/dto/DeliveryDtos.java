package com.farmconnect.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DeliveryDtos {
    private DeliveryDtos() {}

    public record Rates(BigDecimal baseFee, BigDecimal perKmRate, double roadFactor, double maxKm) {}
    public record RatesRequest(@NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal baseFee,
                               @NotNull @DecimalMin("0") @DecimalMax("10000") BigDecimal perKmRate,
                               @DecimalMin("1.0") @DecimalMax("3.0") Double roadFactor,
                               @DecimalMin("1") @DecimalMax("2000") Double maxKm) {}

    /** distanceKm and fee are null when a location is missing; the transporter then confirms the distance. */
    public record Quote(Double distanceKm, BigDecimal fee, BigDecimal baseFee, BigDecimal perKmRate, String pickupPlace, boolean exact) {}
    public record TransporterInfo(Long id, String name, String phone, String county, String operatingCounties, String description, long delivered) {}
    public record Options(Quote quote, List<TransporterInfo> transporters, Long activeJobId) {}

    public record JobRequest(@NotNull Long orderId, @NotNull Long transporterId, Double dropLat, Double dropLng, @Size(max = 500) String note) {}
    public record AcceptRequest(@DecimalMin("0.1") @DecimalMax("2000") Double distanceKm) {}
    public record DeclineRequest(@Size(max = 300) String reason) {}

    public record JobView(Long id, Long orderId, String status, String buyerName, String buyerPhone, String farmerName, String farmerPhone,
                          Long transporterId, String transporterName, String transporterPhone, String pickupPlace, String dropAddress,
                          Double distanceKm, BigDecimal fee, BigDecimal baseFee, BigDecimal perKmRate, boolean feePaid, String note,
                          String declineReason, boolean needsDistance, Instant createdAt, Instant acceptedAt, Instant pickedUpAt,
                          Instant deliveredAt, Instant feePaidAt) {}
}

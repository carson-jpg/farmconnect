package com.farmconnect.dto;

import com.farmconnect.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SupplyDtos {
    private SupplyDtos() {}

    public record ProductRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotNull SupplyCategory category,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 40) String unit,
            @NotNull @DecimalMin("0.01") @DecimalMax("10000000") BigDecimal price,
            @Min(0) @Max(10_000_000) int stockQty,
            @Min(1) @Max(1_000_000) int minOrderQty,
            @Size(max = 100) String licenceRef,
            boolean active) {}

    public record ProductView(Long id, Long companyId, String companyName, String county, String name, SupplyCategory category,
                              String description, String unit, BigDecimal price, int stockQty, int minOrderQty,
                              String licenceRef, boolean active) {}

    public record Line(@NotNull Long productId, @Min(1) @Max(1_000_000) int quantity) {}

    public record PlaceOrderRequest(@NotEmpty @Size(max = 20) List<@Valid Line> items,
                                    @NotBlank @Size(max = 250) String deliveryAddress,
                                    @NotBlank @Size(max = 30) String deliveryPhone,
                                    @Size(max = 500) String deliveryNote) {}

    public record ItemView(Long productId, String name, String unit, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    /** Fee fields are null in the farmer's view: the commission is between FarmConnect and the company. */
    public record OrderView(Long id, SupplyOrderStatus status, String paymentStatus, Long companyId, String companyName,
                            String buyerName, String deliveryPhone, String deliveryAddress, String deliveryNote,
                            List<ItemView> items, BigDecimal total, BigDecimal platformFee, BigDecimal countyFee,
                            BigDecimal companyNet, String rejectionReason, Instant createdAt, Instant deliveredAt, Instant paidAt,
                            String paymentMethod, String mpesaReceipt) {}

    public record CompanySummary(int newOrders, int inProgress, int delivered, int activeProducts, int lowStock,
                                 BigDecimal grossDelivered, BigDecimal feesDelivered, BigDecimal netDelivered,
                                 BigDecimal netUnpaid) {}

    public record PayRequest(@NotBlank @Size(max = 20) String phone) {}

    public record PayoutView(Long id, Long orderId, String companyName, long amountKes, String status, String failureReason,
                             String providerTransactionId, Instant createdAt, Instant completedAt) {}

    public record SupplyPaymentView(Long id, Long orderId, String buyerName, String status, long expectedKes, long paidKes,
                                    long excessKes, String receipt, String phone, String note, Instant createdAt) {}

    public record ConfirmRequest(@NotBlank @Size(max = 30) String receipt, @Min(1) long amountKes) {}

    public record PolicyRequest(@NotNull CommissionType type, @Min(0) @Max(10000) int platformBps,
                                @Min(0) @Max(10000) int countyBps, @Size(max = 200) String countyAgreementRef,
                                @Size(max = 300) String note) {}

    public record PolicyView(Long id, CommissionType type, int platformBps, int countyBps, String countyAgreementRef,
                             Instant effectiveFrom, Instant effectiveTo, String createdBy, String note) {}
}

package com.farmconnect.dto;

import com.farmconnect.model.ProduceRequestStatus;
import com.farmconnect.model.PurchaseOrderStatus;
import com.farmconnect.model.QuotationStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BulkDtos {
    private BulkDtos() {}

    public record RequestForm(
            @NotBlank @Size(max = 80) String produce,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.01") @DecimalMax("100000000") BigDecimal quantity,
            @NotBlank @Size(max = 20) String unit,
            @DecimalMin("0.01") @DecimalMax("10000000") BigDecimal maxPrice,
            @NotBlank @Size(max = 60) String deliveryCounty,
            @NotBlank @Size(max = 250) String deliveryLocation,
            @NotNull LocalDate neededBy,
            @NotNull LocalDate closesAt) {}

    public record RequestView(Long id, Long companyId, String companyName, String companyCounty, String produce, String description,
                              BigDecimal quantity, String unit, BigDecimal maxPrice, String deliveryCounty, String deliveryLocation,
                              LocalDate neededBy, LocalDate closesAt, ProduceRequestStatus status, BigDecimal remaining,
                              long openQuotations, Instant createdAt) {}

    public record QuoteForm(
            @NotNull @DecimalMin("0.01") @DecimalMax("100000000") BigDecimal quantity,
            @NotNull @DecimalMin("0.01") @DecimalMax("10000000") BigDecimal pricePerUnit,
            LocalDate availableFrom,
            @Size(max = 500) String message) {}

    public record QuoteView(Long id, Long requestId, String produce, String unit, String companyName, String farmerName,
                            String farmerPhone, BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal total,
                            LocalDate availableFrom, String message, QuotationStatus status, String rejectionReason, Instant createdAt) {}

    /** contactName / contactPhone are the OTHER party: the farmer for the company, the company's representative for the farmer. */
    public record PurchaseOrderView(Long id, Long requestId, String produce, String unit, String companyName, String farmerName,
                                    String contactName, String contactPhone, BigDecimal quantity, BigDecimal pricePerUnit, BigDecimal total,
                                    BigDecimal platformFee, BigDecimal countyFee, BigDecimal farmerNet, String deliveryCounty,
                                    String deliveryLocation, LocalDate neededBy, PurchaseOrderStatus status, String paymentStatus,
                                    String cancelReason, Instant createdAt, Instant deliveredAt, Instant paidAt) {}
}

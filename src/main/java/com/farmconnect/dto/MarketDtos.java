package com.farmconnect.dto;

import com.farmconnect.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class MarketDtos {
    private MarketDtos() {}

    // ---- team
    public record MemberForm(@NotBlank @Size(max = 100) String name, @Size(max = 30) String phone, @Email @Size(max = 150) String email,
                             @NotNull MemberRole role, boolean active) {}
    public record MemberView(Long id, String name, String phone, String email, MemberRole role, boolean active, Instant createdAt) {}

    // ---- services
    public record ServiceForm(@NotBlank @Size(min = 2, max = 120) String name, @NotNull ServiceCategory category,
                              @Size(max = 1000) String description, @Size(max = 40) String priceBasis,
                              @DecimalMin("0.01") @DecimalMax("10000000") BigDecimal price,
                              @Size(max = 47) List<String> counties, boolean active) {}
    public record ServiceView(Long id, Long companyId, String companyName, String companyCounty, String name, ServiceCategory category,
                              String description, String priceBasis, BigDecimal price, List<String> counties, boolean active) {}

    public record ServiceRequestForm(@NotBlank @Size(max = 1000) String details, LocalDate preferredDate,
                                     @NotBlank @Size(max = 250) String location, @NotBlank @Size(max = 30) String phone) {}
    public record ServiceRequestView(Long id, Long serviceId, String serviceName, ServiceCategory category, String companyName,
                                     String companyPhone, String farmerName, String phone, String details, LocalDate preferredDate,
                                     String location, ServiceRequestStatus status, BigDecimal quotedPrice, String companyNote,
                                     Instant createdAt, Instant updatedAt) {}
    public record AcceptForm(@DecimalMin("0.01") @DecimalMax("10000000") BigDecimal quotedPrice, @Size(max = 500) String note) {}

    // ---- invoice
    public record InvoiceLine(String description, int quantity, String unit, BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record Invoice(String number, Instant issuedAt, String sellerName, String sellerRegNumber, String sellerKraPin,
                          String sellerAddress, String sellerPhone, String sellerEmail, String buyerName, String buyerPhone,
                          String deliveryAddress, List<InvoiceLine> lines, BigDecimal total, String paymentStatus,
                          String paymentMethod, String receipt, Instant paidAt, String orderStatus) {}

    // ---- admin overview
    public record Overview(Map<String, Long> companies, Map<String, Long> supplyOrders, BigDecimal supplyDeliveredGross,
                           Map<String, Long> produceRequests, Map<String, Long> quotations, Map<String, Long> purchaseOrders,
                           Map<String, Long> serviceRequests) {}
}

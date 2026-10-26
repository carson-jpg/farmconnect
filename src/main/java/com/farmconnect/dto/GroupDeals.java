package com.farmconnect.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class GroupDeals {
    private GroupDeals() {}

    public record PoolRequest(@NotNull Long groupId, @NotBlank @Pattern(regexp = "SELL|BUY") String kind,
                              @NotBlank @Size(max = 80) String item, @NotBlank @Size(max = 40) String unit,
                              @NotNull @DecimalMin("0.01") @DecimalMax("100000000") BigDecimal targetQty,
                              @DecimalMin("0") @DecimalMax("100000000") BigDecimal unitPrice,
                              @DecimalMin("0") @DecimalMax("100000000") BigDecimal listPrice,
                              LocalDate closesAt, @Size(max = 1000) String notes) {}
    /** qty 0 takes the member out of the deal. */
    public record ShareRequest(@NotNull @DecimalMin("0") @DecimalMax("100000000") BigDecimal qty) {}
    public record CompleteRequest(@DecimalMin("0") @DecimalMax("100000000") BigDecimal finalUnitPrice, @Size(max = 500) String note) {}

    public record PoolResponse(Long id, Long groupId, String groupName, String subCounty, String kind, String item, String unit,
                               BigDecimal targetQty, BigDecimal committedQty, int members, int progressPct,
                               BigDecimal unitPrice, BigDecimal listPrice, Integer discountPct, String status, LocalDate closesAt,
                               String notes, String finalNote, Long leaderId, String leaderName,
                               BigDecimal myQty, BigDecimal myAmount, boolean canManage, boolean isMember, Instant createdAt) {}
    public record ShareInfo(Long userId, String name, String phone, BigDecimal qty, BigDecimal amount) {}
    public record PoolDetail(PoolResponse pool, List<ShareInfo> shares) {}
}

package com.farmconnect.dto;

import com.farmconnect.model.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class CompanyDtos {
    private CompanyDtos() {}

    public record CompanyRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(min = 3, max = 40) String businessRegNumber,
            @NotBlank String kraPin,
            @NotNull CompanyCategory category,
            boolean canBuy, boolean canSell,
            @NotBlank @Size(max = 100) String repName,
            @NotBlank @Size(max = 30) String repPhone,
            @Email @Size(max = 150) String repEmail,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 250) String address,
            @NotBlank @Size(max = 60) String county,
            @Size(max = 47) List<String> operatingCounties) {}

    public record DocumentView(Long id, CompanyDocType type, String fileName, Instant uploadedAt) {}

    /** What the company itself and the super admin see. */
    public record CompanyView(Long id, String name, String businessRegNumber, String kraPin, CompanyCategory category,
                              boolean canBuy, boolean canSell, String repName, String repPhone, String repEmail,
                              String description, String address, String county, List<String> operatingCounties,
                              CompanyStatus status, String rejectionReason, String suspensionReason,
                              Instant submittedAt, Instant reviewedAt, List<DocumentView> documents,
                              PayoutAccountType payoutType, String payoutAccount, String payoutReference, boolean payoutApproved,
                              /** True only when VERIFIED: the portal unlocks listings and orders. */
                              boolean verified) {}

    public record AdminCompanySummary(Long id, String name, CompanyCategory category, String county,
                                      CompanyStatus status, Instant submittedAt, int documents) {}

    public record AdminCompanyDetail(CompanyView company, String ownerName, String ownerEmail, String ownerPhone,
                                     String reviewedByName) {}

    public record PayoutAccountRequest(@NotNull PayoutAccountType type, @NotBlank @Size(max = 30) String account,
                                       @Size(max = 40) String reference) {}

    public record ReasonRequest(@NotBlank @Size(min = 5, max = 500) String reason) {}
}

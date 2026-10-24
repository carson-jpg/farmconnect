package com.farmconnect.dto;

import com.farmconnect.dto.CompanyDtos.*;
import com.farmconnect.model.Company;
import com.farmconnect.model.CompanyDocument;
import com.farmconnect.model.CompanyStatus;
import java.util.Arrays;
import java.util.List;

public final class CompanyMapper {
    private CompanyMapper() {}

    public static List<String> counties(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static CompanyView view(Company c, List<CompanyDocument> docs) {
        return new CompanyView(c.getId(), c.getName(), c.getBusinessRegNumber(), c.getKraPin(), c.getCategory(),
                c.isCanBuy(), c.isCanSell(), c.getRepName(), c.getRepPhone(), c.getRepEmail(), c.getDescription(),
                c.getAddress(), c.getCounty(), counties(c.getOperatingCounties()), c.getStatus(),
                c.getRejectionReason(), c.getSuspensionReason(), c.getSubmittedAt(), c.getReviewedAt(),
                docs.stream().map(d -> new DocumentView(d.getId(), d.getType(), d.getFileName(), d.getUploadedAt())).toList(),
                c.getPayoutType(), c.getPayoutAccount(), c.getPayoutReference(), c.isPayoutApproved(),
                c.getStatus() == CompanyStatus.VERIFIED);
    }

    public static AdminCompanySummary summary(Company c, int docCount) {
        return new AdminCompanySummary(c.getId(), c.getName(), c.getCategory(), c.getCounty(), c.getStatus(),
                c.getSubmittedAt(), docCount);
    }
}

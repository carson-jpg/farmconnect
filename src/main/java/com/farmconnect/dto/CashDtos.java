package com.farmconnect.dto;

import java.time.Instant;

/** Cash payments recorded in the app, and the commission bills they create. Money is shown in KES. */
public final class CashDtos {
    private CashDtos() {}

    public record RecordRequest(String note) {}
    public record RespondRequest(String note) {}
    public record DecisionRequest(String note) {}
    public record PayBillRequest(String phone) {}
    public record MarkPaidRequest(String note) {}

    public record BillView(Long id, Long cashSaleId, Long orderId, Long farmerId, String farmerName, double saleAmount,
                           double platformAmount, double countyAmount, double amount, String status, String mpesaReceipt,
                           String paidVia, boolean overdue, Instant createdAt, Instant paidAt) {}

    public record CashView(Long id, Long orderId, Long farmerId, String farmerName, String farmerPhone,
                           Long buyerId, String buyerName, String buyerPhone, String orderStatus,
                           double amount, double platformAmount, double countyAmount, double farmerKeeps,
                           String status, String farmerNote, String buyerNote, String decisionNote,
                           Instant recordedAt, Instant buyerRespondedAt, Instant decidedAt, BillView bill) {}
}

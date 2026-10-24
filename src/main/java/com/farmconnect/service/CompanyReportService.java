package com.farmconnect.service;

import com.farmconnect.dto.MarketDtos.Invoice;
import com.farmconnect.dto.MarketDtos.InvoiceLine;
import com.farmconnect.model.*;
import com.farmconnect.repository.PurchaseOrderRepository;
import com.farmconnect.repository.SupplyOrderRepository;
import com.farmconnect.repository.SupplyPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;

/** Invoices and CSV exports for a company's own records. */
@Service
@RequiredArgsConstructor
public class CompanyReportService {
    private static final ZoneId KENYA = ZoneId.of("Africa/Nairobi");

    private final SupplyOrderRepository orders;
    private final SupplyPayoutRepository payouts;
    private final PurchaseOrderRepository purchaseOrders;
    private final CompanyService companies;

    @Transactional(readOnly = true)
    public Invoice invoice(User owner, Long orderId) {
        Company c = companies.entityOf(owner);
        SupplyOrder o = orders.findById(orderId).filter(x -> x.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (o.getStatus() == SupplyOrderStatus.CANCELLED || o.getStatus() == SupplyOrderStatus.REJECTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is no invoice for a " + o.getStatus().name().toLowerCase() + " order");
        List<InvoiceLine> lines = o.getItems().stream().map(i -> new InvoiceLine(i.getProductName(), i.getQuantity(), i.getUnit(),
                i.getUnitPrice(), i.getUnitPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))).toList();
        return new Invoice("INV-" + String.format("%06d", o.getId()), o.getCreatedAt(), c.getName(), c.getBusinessRegNumber(), c.getKraPin(),
                c.getAddress() + (c.getCounty() == null ? "" : ", " + c.getCounty()), c.getRepPhone(), c.getRepEmail(),
                o.getBuyer().getName(), o.getDeliveryPhone(), o.getDeliveryAddress(), lines, o.getTotal(), o.getPaymentStatus(),
                o.getPaymentMethod(), o.getMpesaReceipt(), o.getPaidAt(), o.getStatus().name());
    }

    @Transactional(readOnly = true)
    public String salesCsv(User owner, LocalDate from, LocalDate to) {
        Company c = companies.entityOf(owner);
        StringBuilder b = row(new StringBuilder(), "Order", "Date", "Farmer", "Items", "Total (KSh)", "FarmConnect fee", "County fee", "You receive",
                "Status", "Payment", "Method", "M-Pesa receipt");
        for (SupplyOrder o : orders.findByCompanyIdOrderByCreatedAtDesc(c.getId())) {
            if (!inRange(o.getCreatedAt(), from, to)) continue;
            String items = String.join("; ", o.getItems().stream().map(i -> i.getQuantity() + " x " + i.getProductName()).toList());
            row(b, String.valueOf(o.getId()), o.getCreatedAt().toString(), o.getBuyer().getName(), items, str(o.getTotal()), str(o.getPlatformFee()),
                    str(o.getCountyFee()), str(o.getCompanyNet()), o.getStatus().name(), o.getPaymentStatus(), o.getPaymentMethod(), o.getMpesaReceipt());
        }
        return b.toString();
    }

    @Transactional(readOnly = true)
    public String payoutsCsv(User owner, LocalDate from, LocalDate to) {
        Company c = companies.entityOf(owner);
        StringBuilder b = row(new StringBuilder(), "Payout", "Order", "Amount (KSh)", "Status", "Created", "Completed", "M-Pesa reference", "Note");
        for (SupplyPayout p : payouts.findByCompanyIdOrderByIdDesc(c.getId())) {
            if (!inRange(p.getCreatedAt(), from, to)) continue;
            row(b, String.valueOf(p.getId()), String.valueOf(p.getOrder().getId()), String.valueOf(p.getAmountCents() / 100), p.getStatus().name(),
                    p.getCreatedAt().toString(), p.getCompletedAt() == null ? "" : p.getCompletedAt().toString(), p.getProviderTransactionId(), p.getFailureReason());
        }
        return b.toString();
    }

    @Transactional(readOnly = true)
    public String purchasesCsv(User owner, LocalDate from, LocalDate to) {
        Company c = companies.entityOf(owner);
        StringBuilder b = row(new StringBuilder(), "Order", "Date", "Farmer", "Produce", "Quantity", "Unit", "Price per unit", "Total (KSh)",
                "Status", "Payment", "Deliver to", "Needed by");
        for (PurchaseOrder o : purchaseOrders.findByCompanyIdOrderByCreatedAtDesc(c.getId())) {
            if (!inRange(o.getCreatedAt(), from, to)) continue;
            row(b, String.valueOf(o.getId()), o.getCreatedAt().toString(), o.getFarmer().getName(), o.getProduce(), str(o.getQuantity()), o.getUnit(),
                    str(o.getPricePerUnit()), str(o.getTotal()), o.getStatus().name(), o.getPaymentStatus(),
                    o.getDeliveryCounty() + ", " + o.getDeliveryLocation(), o.getNeededBy() == null ? "" : o.getNeededBy().toString());
        }
        return b.toString();
    }

    private static boolean inRange(Instant t, LocalDate from, LocalDate to) {
        LocalDate d = t.atZone(KENYA).toLocalDate();
        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    private static StringBuilder row(StringBuilder b, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) b.append(',');
            b.append(cell(cells[i]));
        }
        return b.append("\r\n");
    }

    /** Quotes the cell, and defuses spreadsheet formulas (a cell starting with = + - @ would otherwise be executed by Excel). */
    static String cell(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}

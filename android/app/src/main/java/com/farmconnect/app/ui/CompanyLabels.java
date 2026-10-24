package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

/** Names and colours for company categories, statuses and document types. */
final class CompanyLabels {
    private CompanyLabels() {}

    static final String[] CATEGORIES = {"AGRI_SUPPLIER", "PRODUCE_BUYER", "TRANSPORT", "SERVICE_PROVIDER"};
    static final String[] CATEGORY_NAMES = {"Agricultural supplier", "Bulk produce buyer / processor", "Transport and logistics", "Service provider"};

    static final String[] ROLES = {"BOTH", "SELL", "BUY"};
    static final String[] ROLE_NAMES = {"Buys produce and sells", "Sells products or services", "Buys produce"};

    static final String[] DOC_TYPES = {"REGISTRATION_CERT", "KRA_PIN_CERT", "TRADE_LICENCE", "SECTOR_LICENCE", "REP_ID", "OTHER"};
    static final String[] DOC_NAMES = {"Registration certificate (required)", "KRA PIN certificate (required)", "Trade / business licence",
            "Sector licence (KEPHIS, PCPB, NTSA...)", "Representative ID", "Other"};

    static final String[] STATUSES = {"PENDING_VERIFICATION", "UNDER_REVIEW", "VERIFIED", "REJECTED", "SUSPENDED"};
    static final String[] STATUS_NAMES = {"Waiting", "Under review", "Verified", "Rejected", "Suspended"};

    static String name(String[] keys, String[] names, String key) {
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(key)) return I18n.t(names[i]);
        return key == null ? "" : key;
    }

    static String status(String s) { return name(STATUSES, STATUS_NAMES, s); }

    static int statusColor(String s) {
        if (s == null) return 0xFF78909C;
        switch (s) {
            case "VERIFIED": return 0xFF2E7D32;
            case "UNDER_REVIEW": return 0xFF1E88E5;
            case "REJECTED": return 0xFFE53935;
            case "SUSPENDED": return 0xFF78909C;
            default: return 0xFFF9A825;
        }
    }

    static String statusIcon(String s) {
        if (s == null) return "🏢";
        switch (s) {
            case "VERIFIED": return "✅";
            case "UNDER_REVIEW": return "🔎";
            case "REJECTED": return "❌";
            case "SUSPENDED": return "⛔";
            default: return "⏳";
        }
    }

    static final String[] SUPPLY_CATEGORIES = {"SEED", "FERTILIZER", "PESTICIDE", "ANIMAL_FEED", "EQUIPMENT", "OTHER"};
    static final String[] SUPPLY_CATEGORY_NAMES = {"Seed", "Fertiliser", "Pesticide / agrochemical", "Animal feed", "Equipment", "Other"};

    static final String[] ORDER_STATUSES = {"PLACED", "CONFIRMED", "DISPATCHED", "DELIVERED", "CANCELLED", "REJECTED"};
    static final String[] ORDER_STATUS_NAMES = {"New", "Confirmed", "Dispatched", "Delivered", "Cancelled", "Rejected"};

    static final String[] PAYOUT_TYPES = {"MPESA_PHONE", "PAYBILL", "TILL"};
    static final String[] PAYOUT_TYPE_NAMES = {"M-Pesa phone number", "Paybill", "Till number (Buy Goods)"};

    static String money(double v) { return String.format(java.util.Locale.US, "KSh %,.2f", v); }

    static int orderColor(String s) {
        if (s == null) return 0xFF78909C;
        switch (s) {
            case "DELIVERED": return 0xFF2E7D32;
            case "DISPATCHED": return 0xFF1E88E5;
            case "CONFIRMED": return 0xFF00897B;
            case "CANCELLED": case "REJECTED": return 0xFFE53935;
            default: return 0xFFF9A825;
        }
    }

    static String itemsSummary(java.util.List<com.farmconnect.app.data.Models.SupplyItem> items) {
        StringBuilder b = new StringBuilder();
        if (items != null) for (com.farmconnect.app.data.Models.SupplyItem i : items) {
            if (b.length() > 0) b.append(", ");
            b.append(i.quantity).append(" × ").append(i.name);
        }
        return b.toString();
    }

    static String orderDetail(com.farmconnect.app.data.Models.SupplyOrder o, boolean company) {
        StringBuilder m = new StringBuilder();
        m.append(I18n.t("Status: ")).append(name(ORDER_STATUSES, ORDER_STATUS_NAMES, o.status)).append("\n");
        m.append(company ? I18n.t("Farmer: ") + o.buyerName : I18n.t("Company: ") + o.companyName).append("\n");
        m.append(I18n.t("Deliver to: ")).append(o.deliveryAddress).append(" · ").append(o.deliveryPhone).append("\n");
        if (o.deliveryNote != null) m.append(I18n.t("Note: ")).append(o.deliveryNote).append("\n");
        m.append("\n");
        if (o.items != null) for (com.farmconnect.app.data.Models.SupplyItem i : o.items)
            m.append(i.quantity).append(" × ").append(i.name).append(" (").append(i.unit).append(")  ").append(money(i.lineTotal)).append("\n");
        m.append("\n").append(I18n.t("Total: ")).append(money(o.total));
        if (company && o.companyNet != null) {
            m.append("\n").append(I18n.t("FarmConnect fee: ")).append(money((o.platformFee == null ? 0 : o.platformFee) + (o.countyFee == null ? 0 : o.countyFee)));
            m.append("\n").append(I18n.t("You receive: ")).append(money(o.companyNet));
        }
        String pay;
        if ("PAID".equals(o.paymentStatus))
            pay = "MPESA".equals(o.paymentMethod) ? I18n.t("Paid by M-Pesa ") + (o.mpesaReceipt == null ? "" : o.mpesaReceipt) : I18n.t("Paid (cash)");
        else if ("PENDING".equals(o.paymentStatus)) pay = I18n.t("M-Pesa payment in progress");
        else pay = I18n.t("Not yet paid");
        m.append("\n").append(I18n.t("Payment: ")).append(pay);
        if (o.rejectionReason != null) m.append("\n").append(I18n.t("Reason: ")).append(o.rejectionReason);
        return m.toString();
    }

    static final String[] REQ_STATUS_KEYS = {"OPEN", "CLOSED", "FULFILLED", "CANCELLED"};
    static final String[] REQ_STATUS_NAMES = {"Open", "Closed", "Filled", "Cancelled"};
    static final String[] QUOTE_STATUS_KEYS = {"SUBMITTED", "ACCEPTED", "REJECTED", "WITHDRAWN"};
    static final String[] QUOTE_STATUS_NAMES = {"Waiting", "Accepted", "Declined", "Withdrawn"};
    static final String[] PO_STATUS_KEYS = {"ISSUED", "CONFIRMED", "DECLINED", "DELIVERED", "CANCELLED"};
    static final String[] PO_STATUS_NAMES = {"Waiting for farmer", "Confirmed", "Declined", "Delivered", "Cancelled"};

    static String qty(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.US, "%.2f", v); }

    /** yyyy-MM-dd, n days from today */
    static String inDays(int n) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_YEAR, n);
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(c.getTime());
    }

    static int quoteColor(String s) {
        if ("ACCEPTED".equals(s)) return 0xFF2E7D32;
        if ("REJECTED".equals(s) || "WITHDRAWN".equals(s)) return 0xFF78909C;
        return 0xFFF9A825;
    }

    static int poColor(String s) {
        if (s == null) return 0xFF78909C;
        switch (s) {
            case "DELIVERED": return 0xFF2E7D32;
            case "CONFIRMED": return 0xFF00897B;
            case "DECLINED": case "CANCELLED": return 0xFFE53935;
            default: return 0xFFF9A825;
        }
    }

    static String poDetail(com.farmconnect.app.data.Models.BulkOrder o, boolean company) {
        StringBuilder m = new StringBuilder();
        m.append(I18n.t("Status: ")).append(name(PO_STATUS_KEYS, PO_STATUS_NAMES, o.status)).append("\n");
        m.append(company ? I18n.t("Farmer: ") + o.farmerName : I18n.t("Company: ") + o.companyName).append("\n");
        if (o.contactPhone != null) m.append(I18n.t("Contact: ")).append(o.contactName == null ? "" : o.contactName + " · ").append(o.contactPhone).append("\n");
        m.append(o.produce).append(": ").append(qty(o.quantity)).append(" ").append(o.unit).append(" × ").append(money(o.pricePerUnit)).append("\n");
        m.append(I18n.t("Total: ")).append(money(o.total)).append("\n");
        if (o.farmerNet != null && o.platformFee != null && (o.platformFee + (o.countyFee == null ? 0 : o.countyFee)) > 0)
            m.append(I18n.t("FarmConnect fee: ")).append(money(o.platformFee + (o.countyFee == null ? 0 : o.countyFee)))
                    .append("\n").append(I18n.t("Farmer receives: ")).append(money(o.farmerNet)).append("\n");
        m.append(I18n.t("Deliver to: ")).append(o.deliveryCounty).append(", ").append(o.deliveryLocation).append("\n");
        m.append(I18n.t("Needed by: ")).append(o.neededBy).append("\n");
        m.append(I18n.t("Payment: ")).append("PAID".equals(o.paymentStatus) ? I18n.t("Paid") : I18n.t("Not yet paid"));
        if (o.cancelReason != null) m.append("\n").append(I18n.t("Reason: ")).append(o.cancelReason);
        return m.toString();
    }

    static final String[] HIRE_CATEGORIES = {"TRANSPORT", "MACHINERY_HIRE", "SOIL_TESTING", "IRRIGATION", "AGRONOMY", "STORAGE", "OTHER"};
    static final String[] HIRE_CATEGORY_NAMES = {"Transport and logistics", "Machinery hire", "Soil testing", "Irrigation", "Agronomy advice", "Storage", "Other"};
    static final String[] MEMBER_ROLES = {"OWNER", "MANAGER", "SALES", "DRIVER", "ACCOUNTANT", "OTHER"};
    static final String[] MEMBER_ROLE_NAMES = {"Owner", "Manager", "Sales", "Driver", "Accountant", "Other"};
    static final String[] HIRE_STATUS_KEYS = {"REQUESTED", "ACCEPTED", "DECLINED", "COMPLETED", "CANCELLED"};
    static final String[] HIRE_STATUS_NAMES = {"New", "Accepted", "Declined", "Completed", "Cancelled"};

    static int hireColor(String s) {
        if (s == null) return 0xFF78909C;
        switch (s) {
            case "ACCEPTED": return 0xFF1E88E5;
            case "COMPLETED": return 0xFF2E7D32;
            case "DECLINED": return 0xFFE53935;
            case "CANCELLED": return 0xFF78909C;
            default: return 0xFFF9A825;
        }
    }

    static String priceText(Double price, String basis) {
        return price == null ? I18n.t("Price on request") : money(price) + (basis == null || basis.isEmpty() ? "" : " " + basis);
    }
}

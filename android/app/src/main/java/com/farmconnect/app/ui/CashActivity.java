package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import com.farmconnect.app.data.Models.CashBill;
import com.farmconnect.app.data.Models.CashSale;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Cash payments. The farmer records cash received, the buyer confirms it, the super admin approves it. After approval the
 * farmer pays the commission bill by M-Pesa. Extra "orderId" shows one order and lets the farmer record the cash.
 */
public class CashActivity extends BaseListActivity {
    private long orderId() { return getIntent().getLongExtra("orderId", -1); }
    private boolean farmer() { return "FARMER".equals(Session.role()); }

    @Override protected String screenTitle() { return I18n.t("Cash payments"); }
    @Override protected String emptyText() {
        return I18n.t(farmer() ? "No cash payments yet. Record cash from an order page." : "No cash payments to confirm");
    }
    @Override protected String fabText() { return orderId() > 0 && farmer() ? I18n.t("➕ Record cash received") : null; }
    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("note", I18n.t("Note (optional)"), false));
        Forms.show(this, I18n.t("Cash received for this order"), I18n.t("Record"), f, v ->
                Ui.watch(this, vm.cashRecord(orderId(), v.get("note")), b.progress, r -> {
                    Ui.toast(this, I18n.t("Recorded. The buyer will be asked to confirm."));
                    load();
                }));
    }

    private static String statusName(String s) {
        if ("RECORDED".equals(s)) return I18n.t("Waiting for buyer");
        if ("BUYER_CONFIRMED".equals(s)) return I18n.t("Waiting for approval");
        if ("BUYER_DISPUTED".equals(s)) return I18n.t("Buyer disputed");
        if ("APPROVED".equals(s)) return I18n.t("Approved");
        return I18n.t("Rejected");
    }
    private static int statusColor(String s) {
        if ("APPROVED".equals(s)) return 0xFF2E7D32;
        if ("REJECTED".equals(s) || "BUYER_DISPUTED".equals(s)) return 0xFFC62828;
        return 0xFFF9A825;
    }
    private static String billName(String s) {
        if ("PAID".equals(s)) return I18n.t("Commission paid");
        if ("PENDING".equals(s)) return I18n.t("Waiting for M-Pesa");
        return I18n.t("Commission unpaid");
    }

    @Override protected void load() {
        if (vm == null) return;
        if (orderId() > 0) {
            Ui.watch(this, vm.cashForOrder(orderId()), b.progress, sales -> render(sales, new ArrayList<>()));
        } else if (farmer()) {
            Ui.watch(this, vm.cashBills(), b.progress, bills ->
                    Ui.watch(this, vm.cashMine(), b.progress, sales -> render(sales, bills)));
        } else {
            Ui.watch(this, vm.cashMine(), b.progress, sales -> render(sales, new ArrayList<>()));
        }
    }

    private void render(List<CashSale> sales, List<CashBill> bills) {
        List<Row> rows = new ArrayList<>();
        for (CashBill x : bills) {
            if ("PAID".equals(x.status)) continue;
            rows.add(Row.of("🧾", I18n.t("Commission to pay: ") + Ui.kes(x.amount))
                    .sub(I18n.t("Order #") + x.orderId + (x.overdue ? " · " + I18n.t("Overdue") : ""))
                    .badge(billName(x.status), x.overdue ? 0xFFC62828 : 0xFFF9A825)
                    .click(() -> payDialog(x)));
        }
        for (CashSale c : sales) {
            rows.add(Row.of("💵", Ui.kes(c.amount) + " · " + I18n.t("Order #") + c.orderId)
                    .sub(farmer() ? c.buyerName : c.farmerName)
                    .meta(c.bill == null ? "" : billName(c.bill.status))
                    .badge(statusName(c.status), statusColor(c.status))
                    .click(() -> showSale(c)));
        }
        show(rows, sales.size() + I18n.t(" cash payments"));
    }

    private void showSale(CashSale c) {
        boolean buyer = !farmer();
        Sheet s = Sheet.of(this, Ui.kes(c.amount)).badge(statusName(c.status), statusColor(c.status))
                .field(I18n.t("Order"), "#" + c.orderId)
                .field(I18n.t("Farmer"), c.farmerName)
                .field(I18n.t("Buyer"), c.buyerName)
                .note(c.farmerNote);
        if (!buyer) {
            s.divider().field(I18n.t("Commission (platform + county)"), Ui.kes(c.platformAmount + c.countyAmount))
                    .strong(I18n.t("You keep"), Ui.kes(c.farmerKeeps));
        }
        if (c.buyerNote != null && !c.buyerNote.isEmpty()) s.field(I18n.t("Buyer says"), c.buyerNote);
        if (c.decisionNote != null && !c.decisionNote.isEmpty()) s.field(I18n.t("Decision"), c.decisionNote);
        boolean open = "RECORDED".equals(c.status) || "BUYER_CONFIRMED".equals(c.status) || "BUYER_DISPUTED".equals(c.status);
        if (buyer && open) {
            s.primary(I18n.t("Yes, I paid this in cash"), () ->
                    Ui.watch(this, vm.cashConfirm(c.id), b.progress, r -> { Ui.toast(this, I18n.t("Thank you. Waiting for approval.")); load(); }));
            s.danger(I18n.t("No, this is wrong"), () -> dispute(c));
        }
        if (open) s.note(I18n.t("The super admin must approve this before the order can be marked delivered."));
        if (!buyer && c.bill != null && !"PAID".equals(c.bill.status)) s.primary(I18n.t("Pay commission ") + Ui.kes(c.bill.amount), () -> payDialog(c.bill));
        s.show();
    }

    private void dispute(CashSale c) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("note", I18n.t("What is wrong?"), true));
        Forms.show(this, I18n.t("Dispute this payment"), I18n.t("Send"), f, v ->
                Ui.watch(this, vm.cashDispute(c.id, v.get("note")), b.progress, r -> { Ui.toast(this, I18n.t("Sent")); load(); }));
    }

    private void payDialog(CashBill x) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.phone("phone", I18n.t("M-Pesa number (leave blank to use your account number)"), false));
        Forms.show(this, I18n.t("Pay commission ") + Ui.kes(x.amount), I18n.t("Send M-Pesa prompt"), f, v ->
                Ui.watch(this, vm.cashPay(x.id, v.get("phone").isEmpty() ? null : v.get("phone")), b.progress, r -> {
                    Ui.toast(this, I18n.t("Check your phone and enter your M-Pesa PIN"));
                    load();
                }));
    }
}

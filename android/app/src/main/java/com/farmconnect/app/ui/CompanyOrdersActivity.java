package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Invoice;
import com.farmconnect.app.data.Models.InvoiceLine;
import com.farmconnect.app.data.Models.SupplyOrder;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.List;

/** Orders from farmers: confirm, dispatch, deliver, reject, mark paid, share the invoice. */
public class CompanyOrdersActivity extends BaseListActivity {
    private SupplyViewModel svm;
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("Orders"); }
    @Override protected String emptyText() { return I18n.t("No orders here"); }

    @Override protected String[][] chips() {
        String[][] c = new String[CompanyLabels.ORDER_STATUSES.length + 1][];
        c[0] = new String[]{"", "All"};
        for (int i = 0; i < CompanyLabels.ORDER_STATUSES.length; i++)
            c[i + 1] = new String[]{CompanyLabels.ORDER_STATUSES[i], CompanyLabels.ORDER_STATUS_NAMES[i]};
        return c;
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        svm = new ViewModelProvider(this).get(SupplyViewModel.class);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (svm == null) return;
        Ui.watch(this, svm.companyOrders(chip), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (SupplyOrder o : list) {
                rows.add(Row.of("🧾", "#" + o.id + " · " + o.buyerName)
                        .sub(CompanyLabels.itemsSummary(o.items))
                        .meta(CompanyLabels.money(o.total) + " · " + Ui.dateTime(o.createdAt))
                        .badge(CompanyLabels.name(CompanyLabels.ORDER_STATUSES, CompanyLabels.ORDER_STATUS_NAMES, o.status), CompanyLabels.orderColor(o.status))
                        .click(() -> detail(o)));
            }
            show(rows, list.size() + I18n.t(" orders"));
        });
    }

    private void detail(SupplyOrder o) {
        Sheet s = Sheet.of(this, I18n.t("Order #") + o.id)
                .badge(CompanyLabels.name(CompanyLabels.ORDER_STATUSES, CompanyLabels.ORDER_STATUS_NAMES, o.status), CompanyLabels.orderColor(o.status))
                .field(I18n.t("Farmer"), o.buyerName + (o.deliveryPhone == null ? "" : " · " + o.deliveryPhone))
                .field(I18n.t("Deliver to"), o.deliveryAddress)
                .field(I18n.t("Note"), o.deliveryNote)
                .field(I18n.t("Items"), itemLines(o))
                .strong(I18n.t("Order total"), CompanyLabels.money(o.total));
        if (o.companyNet != null) {
            s.field(I18n.t("FarmConnect fee"), CompanyLabels.money((o.platformFee == null ? 0 : o.platformFee) + (o.countyFee == null ? 0 : o.countyFee)));
            s.strong(I18n.t("You receive"), CompanyLabels.money(o.companyNet));
        }
        s.field(I18n.t("Payment"), paymentText(o));
        s.field(I18n.t("Reason"), o.rejectionReason);
        switch (o.status) {
            case "PLACED": s.primary(I18n.t("Confirm order"), () -> go(svm.step("confirm", o.id), I18n.t("Order confirmed"))); break;
            case "CONFIRMED": s.primary(I18n.t("Mark dispatched"), () -> go(svm.step("dispatch", o.id), I18n.t("Marked dispatched"))); break;
            case "DISPATCHED": s.primary(I18n.t("Mark delivered"), () -> go(svm.step("deliver", o.id), I18n.t("Marked delivered"))); break;
            case "DELIVERED":
                if ("UNPAID".equals(o.paymentStatus)) s.primary(I18n.t("Mark paid (cash)"), () -> confirmPaid(o));
                break;
            default: break;
        }
        if (!"CANCELLED".equals(o.status) && !"REJECTED".equals(o.status)) s.secondary(I18n.t("Share invoice"), () -> shareInvoice(o));
        if ("PLACED".equals(o.status) || "CONFIRMED".equals(o.status)) s.danger(I18n.t("Reject order"), () -> reject(o));
        s.show();
    }

    private static String itemLines(SupplyOrder o) {
        StringBuilder b = new StringBuilder();
        if (o.items != null) for (com.farmconnect.app.data.Models.SupplyItem i : o.items) {
            if (b.length() > 0) b.append('\n');
            b.append(i.quantity).append(" × ").append(i.name).append(" (").append(i.unit).append(")  ").append(CompanyLabels.money(i.lineTotal));
        }
        return b.toString();
    }

    private static String paymentText(SupplyOrder o) {
        if ("PAID".equals(o.paymentStatus)) return "MPESA".equals(o.paymentMethod) ? I18n.t("Paid by M-Pesa ") + (o.mpesaReceipt == null ? "" : o.mpesaReceipt) : I18n.t("Paid (cash)");
        if ("PENDING".equals(o.paymentStatus)) return I18n.t("M-Pesa payment in progress");
        return I18n.t("Not yet paid");
    }

    private void confirmPaid(SupplyOrder o) {
        new AlertDialog.Builder(this).setTitle(I18n.t("Mark paid?"))
                .setMessage(I18n.t("Confirm you received ") + CompanyLabels.money(o.total) + I18n.t(" in cash or directly from the farmer. M-Pesa payments through FarmConnect are recorded automatically."))
                .setPositiveButton(I18n.t("Yes, paid"), (x, y) -> go(svm.step("paid", o.id), I18n.t("Marked paid")))
                .setNegativeButton(I18n.t("Cancel"), null).show();
    }

    private void reject(SupplyOrder o) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("reason", I18n.t("Reason shown to the farmer"), true));
        Forms.show(this, I18n.t("Reject order"), I18n.t("Reject"), f, v -> {
            String r = v.get("reason").trim();
            if (r.length() < 5) Ui.toast(this, I18n.t("Please give a reason of at least 5 characters"));
            else go(svm.reject(o.id, r), I18n.t("Order rejected"));
        });
    }

    /** Builds a plain-text invoice and opens the share sheet (WhatsApp, email, print...). */
    private void shareInvoice(SupplyOrder o) {
        Ui.watch(this, mvm.invoice(o.id), b.progress, (Invoice v) -> {
            StringBuilder t = new StringBuilder();
            t.append("INVOICE ").append(v.number).append('\n').append(v.sellerName).append('\n');
            t.append(I18n.t("Reg. no. ")).append(v.sellerRegNumber).append("  KRA PIN ").append(v.sellerKraPin == null ? "-" : v.sellerKraPin).append('\n');
            if (v.sellerAddress != null) t.append(v.sellerAddress).append('\n');
            t.append('\n').append(I18n.t("Bill to: ")).append(v.buyerName).append('\n');
            if (v.deliveryAddress != null) t.append(I18n.t("Deliver to: ")).append(v.deliveryAddress).append('\n');
            t.append('\n');
            if (v.lines != null) for (InvoiceLine l : v.lines)
                t.append(l.quantity).append(" × ").append(l.description).append(" @ ").append(CompanyLabels.money(l.unitPrice)).append(" = ").append(CompanyLabels.money(l.lineTotal)).append('\n');
            t.append('\n').append(I18n.t("TOTAL: ")).append(CompanyLabels.money(v.total)).append('\n');
            t.append("PAID".equals(v.paymentStatus) ? I18n.t("Status: PAID") + ("MPESA".equals(v.paymentMethod) ? " (M-Pesa " + (v.receipt == null ? "" : v.receipt) + ")" : "") : I18n.t("Status: UNPAID"));
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Invoice " + v.number).putExtra(Intent.EXTRA_TEXT, t.toString());
            startActivity(Intent.createChooser(send, I18n.t("Share invoice")));
        });
    }

    private void go(LiveData<Resource<SupplyOrder>> call, String ok) {
        Ui.watch(this, call, b.progress, o -> { Ui.toast(this, ok); load(); });
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkOrder;
import com.farmconnect.app.data.Models.BulkQuote;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BulkViewModel;
import java.util.ArrayList;
import java.util.List;

/** A farmer's quotations (withdraw while waiting) and purchase orders (confirm or decline). */
public class MyBulkActivity extends BaseListActivity {
    private BulkViewModel bvm;

    @Override protected String screenTitle() { return I18n.t("Bulk selling"); }
    @Override protected String emptyText() { return I18n.t("Nothing here yet"); }

    @Override protected String[][] chips() {
        return new String[][]{{"", "Quotations"}, {"ORDERS", "Purchase orders"}};
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        bvm = new ViewModelProvider(this).get(BulkViewModel.class);
    }

    @Override protected void load() {
        if (bvm == null) return;
        if ("ORDERS".equals(chip)) loadOrders(); else loadQuotes();
    }

    private void loadQuotes() {
        Ui.watch(this, bvm.myQuotes(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (BulkQuote q : list) {
                rows.add(Row.of("💬", q.produce + " · " + q.companyName)
                        .sub(CompanyLabels.qty(q.quantity) + " " + q.unit + " × " + CompanyLabels.money(q.pricePerUnit) + " = " + CompanyLabels.money(q.total))
                        .meta(q.rejectionReason == null ? Ui.dateTime(q.createdAt) : I18n.t("Reason: ") + q.rejectionReason)
                        .badge(CompanyLabels.name(CompanyLabels.QUOTE_STATUS_KEYS, CompanyLabels.QUOTE_STATUS_NAMES, q.status), CompanyLabels.quoteColor(q.status))
                        .click(() -> quoteDetail(q)));
            }
            show(rows, list.size() + I18n.t(" quotations"));
        });
    }

    private void quoteDetail(BulkQuote q) {
        Sheet s = Sheet.of(this, q.produce + " · " + q.companyName)
                .badge(CompanyLabels.name(CompanyLabels.QUOTE_STATUS_KEYS, CompanyLabels.QUOTE_STATUS_NAMES, q.status), CompanyLabels.quoteColor(q.status))
                .field(I18n.t("Your offer"), CompanyLabels.qty(q.quantity) + " " + q.unit + " × " + CompanyLabels.money(q.pricePerUnit))
                .field(I18n.t("Available from"), q.availableFrom)
                .note(q.message)
                .field(I18n.t("Reason"), q.rejectionReason)
                .strong(I18n.t("Total"), CompanyLabels.money(q.total));
        if ("SUBMITTED".equals(q.status)) s.danger(I18n.t("Withdraw quotation"), () -> go(bvm.withdraw(q.id), I18n.t("Withdrawn")));
        s.show();
    }

    private void loadOrders() {
        Ui.watch(this, bvm.myOrders(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (BulkOrder o : list) {
                rows.add(Row.of("📑", "#" + o.id + " · " + o.produce + " · " + o.companyName)
                        .sub(CompanyLabels.qty(o.quantity) + " " + o.unit + " · " + CompanyLabels.money(o.total))
                        .meta(I18n.t("Needed by ") + o.neededBy + ("PAID".equals(o.paymentStatus) ? " · " + I18n.t("Paid") : ""))
                        .badge(CompanyLabels.name(CompanyLabels.PO_STATUS_KEYS, CompanyLabels.PO_STATUS_NAMES, o.status), CompanyLabels.poColor(o.status))
                        .click(() -> orderDetail(o)));
            }
            show(rows, list.size() + I18n.t(" orders"));
        });
    }

    private void orderDetail(BulkOrder o) {
        Sheet s = Sheet.of(this, I18n.t("Purchase order #") + o.id)
                .badge(CompanyLabels.name(CompanyLabels.PO_STATUS_KEYS, CompanyLabels.PO_STATUS_NAMES, o.status), CompanyLabels.poColor(o.status))
                .field(I18n.t("Company"), o.companyName)
                .field(I18n.t("Contact"), (o.contactName == null ? "" : o.contactName + " · ") + (o.contactPhone == null ? "" : o.contactPhone))
                .field(I18n.t("Produce"), CompanyLabels.qty(o.quantity) + " " + o.unit + " × " + CompanyLabels.money(o.pricePerUnit))
                .field(I18n.t("Deliver to"), o.deliveryCounty + ", " + o.deliveryLocation)
                .field(I18n.t("Needed by"), o.neededBy)
                .field(I18n.t("Payment"), "PAID".equals(o.paymentStatus) ? I18n.t("Paid") : I18n.t("Not yet paid"))
                .field(I18n.t("Reason"), o.cancelReason)
                .strong(I18n.t("Total"), CompanyLabels.money(o.total));
        if (o.farmerNet != null && o.platformFee != null && (o.platformFee + (o.countyFee == null ? 0 : o.countyFee)) > 0)
            s.field(I18n.t("FarmConnect fee"), CompanyLabels.money(o.platformFee + (o.countyFee == null ? 0 : o.countyFee))).strong(I18n.t("You receive"), CompanyLabels.money(o.farmerNet));
        if ("ISSUED".equals(o.status)) {
            s.primary(I18n.t("Confirm purchase order"), () -> go(bvm.confirm(o.id), I18n.t("Purchase order confirmed")));
            s.danger(I18n.t("Decline"), () -> {
                List<Forms.Field> f = new ArrayList<>();
                f.add(Forms.multi("reason", I18n.t("Reason (optional)"), false));
                Forms.show(this, I18n.t("Decline purchase order"), I18n.t("Decline"), f, v -> go(bvm.decline(o.id, v.get("reason")), I18n.t("Declined")));
            });
        }
        s.show();
    }

    private <T> void go(LiveData<Resource<T>> call, String ok) {
        Ui.watch(this, call, b.progress, r -> { Ui.toast(this, ok); load(); });
    }
}

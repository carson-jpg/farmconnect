package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkOrder;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BulkViewModel;
import java.util.ArrayList;
import java.util.List;

/** Purchase orders the company issued: record delivery, record payment, or cancel. */
public class CompanyBulkOrdersActivity extends BaseListActivity {
    private BulkViewModel bvm;

    @Override protected String screenTitle() { return I18n.t("Purchase orders"); }
    @Override protected String emptyText() { return I18n.t("No purchase orders yet.\nThey are created when you accept a quotation."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        bvm = new ViewModelProvider(this).get(BulkViewModel.class);
    }

    @Override protected void load() {
        if (bvm == null) return;
        Ui.watch(this, bvm.companyOrders(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (BulkOrder o : list) {
                rows.add(Row.of("📑", "#" + o.id + " · " + o.produce + " · " + o.farmerName)
                        .sub(CompanyLabels.qty(o.quantity) + " " + o.unit + " · " + CompanyLabels.money(o.total))
                        .meta(I18n.t("Needed by ") + o.neededBy + ("PAID".equals(o.paymentStatus) ? " · " + I18n.t("Paid") : ""))
                        .badge(CompanyLabels.name(CompanyLabels.PO_STATUS_KEYS, CompanyLabels.PO_STATUS_NAMES, o.status), CompanyLabels.poColor(o.status))
                        .click(() -> detail(o)));
            }
            show(rows, list.size() + I18n.t(" orders"));
        });
    }

    private void detail(BulkOrder o) {
        Sheet s = Sheet.of(this, I18n.t("Purchase order #") + o.id)
                .badge(CompanyLabels.name(CompanyLabels.PO_STATUS_KEYS, CompanyLabels.PO_STATUS_NAMES, o.status), CompanyLabels.poColor(o.status))
                .field(I18n.t("Farmer"), o.farmerName + (o.contactPhone == null ? "" : " · " + o.contactPhone))
                .field(I18n.t("Produce"), CompanyLabels.qty(o.quantity) + " " + o.unit + " × " + CompanyLabels.money(o.pricePerUnit))
                .field(I18n.t("Deliver to"), o.deliveryCounty + ", " + o.deliveryLocation)
                .field(I18n.t("Needed by"), o.neededBy)
                .field(I18n.t("Payment"), "PAID".equals(o.paymentStatus) ? I18n.t("Paid") : I18n.t("Not yet paid"))
                .field(I18n.t("Reason"), o.cancelReason)
                .strong(I18n.t("Total"), CompanyLabels.money(o.total));
        if ("CONFIRMED".equals(o.status)) s.primary(I18n.t("Mark received"), () -> go(bvm.step("deliver", o.id), I18n.t("Delivery recorded")));
        if ("DELIVERED".equals(o.status) && !"PAID".equals(o.paymentStatus))
            s.primary(I18n.t("Mark paid"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Mark paid?")).setMessage(I18n.t("Confirm you paid ") + o.farmerName + " " + CompanyLabels.money(o.total) + ".")
                    .setPositiveButton(I18n.t("Yes, paid"), (a, c) -> go(bvm.step("paid", o.id), I18n.t("Marked paid")))
                    .setNegativeButton(I18n.t("Cancel"), null).show());
        if ("ISSUED".equals(o.status) || "CONFIRMED".equals(o.status))
            s.danger(I18n.t("Cancel order"), () -> {
                List<Forms.Field> f = new ArrayList<>();
                f.add(Forms.multi("reason", I18n.t("Reason shown to the farmer"), true));
                Forms.show(this, I18n.t("Cancel purchase order"), I18n.t("Cancel order"), f, v -> {
                    String r = v.get("reason").trim();
                    if (r.length() < 5) Ui.toast(this, I18n.t("Please give a reason of at least 5 characters"));
                    else go(bvm.cancelOrder(o.id, r), I18n.t("Cancelled"));
                });
            });
        s.show();
    }

    private void go(LiveData<Resource<BulkOrder>> call, String ok) {
        Ui.watch(this, call, b.progress, o -> { Ui.toast(this, ok); load(); });
    }
}

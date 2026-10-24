package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkRequest;
import com.farmconnect.app.data.Models.BulkRequestForm;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BulkViewModel;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;

/** A buying company's produce requests. Verified farmers quote against them. */
public class CompanyRequestsActivity extends BaseListActivity {
    private BulkViewModel bvm;
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("Produce requests"); }
    @Override protected String fabText() { return I18n.t("New request"); }
    @Override protected String emptyText() { return I18n.t("No requests yet.\nPost what you want to buy."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        bvm = new ViewModelProvider(this).get(BulkViewModel.class);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (bvm == null) return;
        Ui.watch(this, bvm.companyRequests(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (BulkRequest r : list) {
                rows.add(Row.of("🌽", r.produce + " · " + CompanyLabels.qty(r.quantity) + " " + r.unit)
                        .sub(I18n.t("Still needed: ") + CompanyLabels.qty(r.remaining) + " " + r.unit + " · " + r.deliveryCounty)
                        .meta(I18n.t("Closes ") + r.closesAt + " · " + r.openQuotations + I18n.t(" quotes waiting"))
                        .badge(CompanyLabels.name(CompanyLabels.REQ_STATUS_KEYS, CompanyLabels.REQ_STATUS_NAMES, r.status), "OPEN".equals(r.status) ? 0xFF2E7D32 : 0xFF78909C)
                        .click(() -> detail(r)));
            }
            show(rows, list.size() + I18n.t(" requests"));
        });
    }

    private void detail(BulkRequest r) {
        Sheet s = Sheet.of(this, r.produce)
                .badge(CompanyLabels.name(CompanyLabels.REQ_STATUS_KEYS, CompanyLabels.REQ_STATUS_NAMES, r.status), "OPEN".equals(r.status) ? 0xFF2E7D32 : 0xFF78909C)
                .field(I18n.t("Quantity"), CompanyLabels.qty(r.quantity) + " " + r.unit + "  (" + I18n.t("still needed ") + CompanyLabels.qty(r.remaining) + ")")
                .field(I18n.t("Highest price"), r.maxPrice == null ? null : CompanyLabels.money(r.maxPrice) + " / " + r.unit)
                .field(I18n.t("Deliver to"), r.deliveryCounty + ", " + r.deliveryLocation)
                .field(I18n.t("Needed by"), r.neededBy)
                .field(I18n.t("Quotations close"), r.closesAt)
                .note(r.description);
        s.primary(I18n.t("View quotations") + (r.openQuotations > 0 ? " (" + r.openQuotations + ")" : ""),
                () -> startActivity(new Intent(this, CompanyQuotesActivity.class).putExtra("requestId", r.id).putExtra("title", r.produce)));
        if ("OPEN".equals(r.status) && r.openQuotations == 0) s.secondary(I18n.t("Edit request"), () -> form(r));
        if ("OPEN".equals(r.status))
            s.danger(I18n.t("Close request"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Close this request?")).setMessage(I18n.t("Waiting quotations will be declined."))
                    .setPositiveButton(I18n.t("Close request"), (a, c) -> Ui.watch(this, bvm.close(r.id), b.progress, res -> { Ui.toast(this, I18n.t("Closed")); load(); }))
                    .setNegativeButton(I18n.t("Cancel"), null).show());
        s.show();
    }

    @Override protected void onFab() { form(null); }

    private void form(BulkRequest e) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("produce", I18n.t("Produce (e.g. Maize)"), true).value(e == null ? "" : e.produce));
        f.add(Forms.number("qty", I18n.t("Quantity needed"), true).value(e == null ? "" : CompanyLabels.qty(e.quantity)));
        f.add(Forms.text("unit", I18n.t("Unit (kg, bag, tonne)"), true).value(e == null ? "kg" : e.unit));
        f.add(Forms.number("max", I18n.t("Highest price per unit (KSh, optional)"), false).value(e == null || e.maxPrice == null ? "" : CompanyLabels.qty(e.maxPrice)));
        f.add(Forms.choice("county", I18n.t("Deliver to county"), true, Labels.COUNTIES, Labels.COUNTIES).value(e == null ? "" : e.deliveryCounty));
        f.add(Forms.text("location", I18n.t("Delivery location"), true).value(e == null ? "" : e.deliveryLocation));
        f.add(Forms.text("closes", I18n.t("Quotations close on (YYYY-MM-DD)"), true).value(e == null ? CompanyLabels.inDays(7) : e.closesAt));
        f.add(Forms.text("needed", I18n.t("Needed by (YYYY-MM-DD)"), true).value(e == null ? CompanyLabels.inDays(14) : e.neededBy));
        f.add(Forms.multi("description", I18n.t("Details (quality, packaging...)"), false).value(e == null ? "" : e.description));
        Forms.show(this, e == null ? I18n.t("New produce request") : I18n.t("Edit request"), I18n.t("Save"), f, v -> {
            BulkRequestForm r = new BulkRequestForm();
            try {
                r.quantity = Double.parseDouble(v.get("qty"));
                String max = v.get("max").trim();
                r.maxPrice = max.isEmpty() ? null : Double.valueOf(max);
            } catch (NumberFormatException x) {
                Ui.toast(this, I18n.t("Check the quantity and price numbers"));
                return;
            }
            r.produce = v.get("produce");
            r.unit = v.get("unit");
            r.deliveryCounty = v.get("county");
            r.deliveryLocation = v.get("location");
            r.closesAt = v.get("closes").trim();
            r.neededBy = v.get("needed").trim();
            r.description = v.get("description");
            Ui.watch(this, e == null ? bvm.create(r) : mvm.updateRequest(e.id, r), b.progress, x -> {
                Ui.toast(this, I18n.t("Request saved"));
                load();
            });
        });
    }
}

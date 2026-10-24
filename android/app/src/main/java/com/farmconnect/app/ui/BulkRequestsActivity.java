package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkQuoteForm;
import com.farmconnect.app.data.Models.BulkRequest;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BulkViewModel;
import java.util.ArrayList;
import java.util.List;

/** Farmers browse companies' bulk requests and send quotations. Only verified farmers can quote. */
public class BulkRequestsActivity extends BaseListActivity {
    private BulkViewModel bvm;

    @Override protected String screenTitle() { return I18n.t("Bulk buyers"); }
    @Override protected boolean searchable() { return true; }
    @Override protected String fabText() { return I18n.t("My quotations"); }
    @Override protected String emptyText() { return I18n.t("No open requests right now"); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        bvm = new ViewModelProvider(this).get(BulkViewModel.class);
    }

    @Override protected void load() {
        Ui.watch(this, bvm.browse(query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (BulkRequest r : list) {
                rows.add(Row.of("🏭", r.produce + " · " + CompanyLabels.qty(r.remaining) + " " + r.unit)
                        .sub(r.companyName + " · " + I18n.t("deliver to ") + r.deliveryCounty)
                        .meta((r.maxPrice == null ? "" : I18n.t("up to ") + CompanyLabels.money(r.maxPrice) + " / " + r.unit + " · ") + I18n.t("quote by ") + r.closesAt)
                        .click(() -> quote(r)));
            }
            show(rows, list.size() + I18n.t(" requests"));
        });
    }

    @Override protected void onFab() { startActivity(new Intent(this, MyBulkActivity.class)); }

    private void quote(BulkRequest r) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.number("qty", I18n.t("How much can you supply (") + r.unit + ")", true).value(CompanyLabels.qty(r.remaining)));
        f.add(Forms.number("price", I18n.t("Your price per ") + r.unit + I18n.t(" (KSh)"), true));
        f.add(Forms.text("from", I18n.t("Available from (YYYY-MM-DD)"), false).value(CompanyLabels.inDays(3)));
        f.add(Forms.multi("message", I18n.t("Message (quality, harvest date...)"), false));
        Forms.show(this, r.produce + " · " + r.companyName, I18n.t("Send quotation"), f, v -> {
            BulkQuoteForm q = new BulkQuoteForm();
            try {
                q.quantity = Double.parseDouble(v.get("qty"));
                q.pricePerUnit = Double.parseDouble(v.get("price"));
            } catch (NumberFormatException x) {
                Ui.toast(this, I18n.t("Check the quantity and price numbers"));
                return;
            }
            String from = v.get("from").trim();
            q.availableFrom = from.isEmpty() ? null : from;
            q.message = v.get("message");
            Ui.watch(this, bvm.sendQuote(r.id, q), b.progress, res -> {
                Ui.toast(this, I18n.t("Quotation sent"));
                load();
            });
        });
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkQuote;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BulkViewModel;
import java.util.ArrayList;
import java.util.List;

/** Quotations farmers sent for one request, cheapest first. Accepting one issues a purchase order. */
public class CompanyQuotesActivity extends BaseListActivity {
    private BulkViewModel bvm;
    private long requestId;

    @Override protected String screenTitle() {
        String t = getIntent().getStringExtra("title");
        return I18n.t("Quotations") + (t == null ? "" : " · " + t);
    }
    @Override protected String emptyText() { return I18n.t("No quotations yet"); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        requestId = getIntent().getLongExtra("requestId", 0);
        bvm = new ViewModelProvider(this).get(BulkViewModel.class);
    }

    @Override protected void load() {
        if (bvm == null) return;
        Ui.watch(this, bvm.quotations(requestId), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (BulkQuote q : list) {
                rows.add(Row.of("🧑‍🌾", q.farmerName + " · " + CompanyLabels.money(q.pricePerUnit) + " / " + q.unit)
                        .sub(CompanyLabels.qty(q.quantity) + " " + q.unit + " · " + I18n.t("total ") + CompanyLabels.money(q.total))
                        .meta((q.availableFrom == null ? "" : I18n.t("Available ") + q.availableFrom + " · ") + (q.farmerPhone == null ? "" : q.farmerPhone))
                        .badge(CompanyLabels.name(CompanyLabels.QUOTE_STATUS_KEYS, CompanyLabels.QUOTE_STATUS_NAMES, q.status), CompanyLabels.quoteColor(q.status))
                        .click(() -> detail(q)));
            }
            show(rows, list.size() + I18n.t(" quotations"));
        });
    }

    private void detail(BulkQuote q) {
        Sheet s = Sheet.of(this, q.farmerName)
                .badge(CompanyLabels.name(CompanyLabels.QUOTE_STATUS_KEYS, CompanyLabels.QUOTE_STATUS_NAMES, q.status), CompanyLabels.quoteColor(q.status))
                .field(I18n.t("Offer"), CompanyLabels.qty(q.quantity) + " " + q.unit + " × " + CompanyLabels.money(q.pricePerUnit))
                .field(I18n.t("Available from"), q.availableFrom)
                .field(I18n.t("Phone"), q.farmerPhone)
                .note(q.message)
                .field(I18n.t("Reason"), q.rejectionReason)
                .strong(I18n.t("Total"), CompanyLabels.money(q.total));
        if ("SUBMITTED".equals(q.status)) {
            s.primary(I18n.t("Accept quotation"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Accept this quotation?"))
                    .setMessage(I18n.t("This issues a purchase order for ") + CompanyLabels.money(q.total) + ".")
                    .setPositiveButton(I18n.t("Accept"), (a, c) -> Ui.watch(this, bvm.accept(q.id), b.progress, res -> {
                        Ui.toast(this, I18n.t("Accepted. Purchase order issued."));
                        load();
                    }))
                    .setNegativeButton(I18n.t("Cancel"), null).show());
            s.danger(I18n.t("Decline"), () -> {
                List<Forms.Field> f = new ArrayList<>();
                f.add(Forms.multi("reason", I18n.t("Reason (optional)"), false));
                Forms.show(this, I18n.t("Decline quotation"), I18n.t("Decline"), f, v ->
                        Ui.watch(this, bvm.rejectQuote(q.id, v.get("reason")), b.progress, res -> { Ui.toast(this, I18n.t("Declined")); load(); }));
            });
        }
        s.show();
    }
}

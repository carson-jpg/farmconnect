package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.HireRequest;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;

/** A farmer's service requests and what the companies answered. */
public class MyServiceRequestsActivity extends BaseListActivity {
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("My service requests"); }
    @Override protected String emptyText() { return I18n.t("You have not requested any services yet"); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (mvm == null) return;
        Ui.watch(this, mvm.myRequests(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (HireRequest r : list) {
                rows.add(Row.of("📨", r.serviceName + " · " + r.companyName)
                        .sub(r.location + (r.preferredDate == null ? "" : " · " + r.preferredDate))
                        .meta(r.quotedPrice == null ? Ui.dateTime(r.createdAt) : I18n.t("Agreed price ") + CompanyLabels.money(r.quotedPrice))
                        .badge(CompanyLabels.name(CompanyLabels.HIRE_STATUS_KEYS, CompanyLabels.HIRE_STATUS_NAMES, r.status), CompanyLabels.hireColor(r.status))
                        .click(() -> detail(r)));
            }
            show(rows, list.size() + I18n.t(" requests"));
        });
    }

    private void detail(HireRequest r) {
        Sheet s = Sheet.of(this, r.serviceName)
                .badge(CompanyLabels.name(CompanyLabels.HIRE_STATUS_KEYS, CompanyLabels.HIRE_STATUS_NAMES, r.status), CompanyLabels.hireColor(r.status))
                .field(I18n.t("Company"), r.companyName + (r.companyPhone == null ? "" : " · " + r.companyPhone))
                .field(I18n.t("Location"), r.location)
                .field(I18n.t("Preferred date"), r.preferredDate)
                .note(r.details)
                .field(I18n.t("Agreed price"), r.quotedPrice == null ? null : CompanyLabels.money(r.quotedPrice))
                .field(I18n.t("Message from the company"), r.companyNote);
        if ("REQUESTED".equals(r.status) || "ACCEPTED".equals(r.status))
            s.danger(I18n.t("Cancel request"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Cancel this request?"))
                    .setPositiveButton(I18n.t("Cancel request"), (x, y) -> Ui.watch(this, mvm.cancel(r.id), b.progress, res -> { Ui.toast(this, I18n.t("Request cancelled")); load(); }))
                    .setNegativeButton(I18n.t("Keep"), null).show());
        s.show();
    }
}

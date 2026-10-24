package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.HireAccept;
import com.farmconnect.app.data.Models.HireRequest;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;

/** Farmers asking for the company's services: accept (with a price), decline, complete. */
public class CompanyServiceRequestsActivity extends BaseListActivity {
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("Service requests"); }
    @Override protected String emptyText() { return I18n.t("No requests here"); }

    @Override protected String[][] chips() {
        return new String[][]{{"", "All"}, {"REQUESTED", "New"}, {"ACCEPTED", "Accepted"}, {"COMPLETED", "Completed"}, {"DECLINED", "Declined"}};
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (mvm == null) return;
        Ui.watch(this, mvm.serviceRequests(chip), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (HireRequest r : list) {
                rows.add(Row.of("📨", r.serviceName + " · " + r.farmerName)
                        .sub(r.location + (r.preferredDate == null ? "" : " · " + r.preferredDate))
                        .meta(Ui.dateTime(r.createdAt))
                        .badge(CompanyLabels.name(CompanyLabels.HIRE_STATUS_KEYS, CompanyLabels.HIRE_STATUS_NAMES, r.status), CompanyLabels.hireColor(r.status))
                        .click(() -> detail(r)));
            }
            show(rows, list.size() + I18n.t(" requests"));
        });
    }

    private void detail(HireRequest r) {
        Sheet s = Sheet.of(this, r.serviceName)
                .badge(CompanyLabels.name(CompanyLabels.HIRE_STATUS_KEYS, CompanyLabels.HIRE_STATUS_NAMES, r.status), CompanyLabels.hireColor(r.status))
                .field(I18n.t("Farmer"), r.farmerName + " · " + r.phone)
                .field(I18n.t("Location"), r.location)
                .field(I18n.t("Preferred date"), r.preferredDate)
                .note(r.details)
                .field(I18n.t("Agreed price"), r.quotedPrice == null ? null : CompanyLabels.money(r.quotedPrice))
                .field(I18n.t("Your note"), r.companyNote);
        if ("REQUESTED".equals(r.status)) {
            s.primary(I18n.t("Accept request"), () -> accept(r));
            s.danger(I18n.t("Decline"), () -> decline(r));
        }
        if ("ACCEPTED".equals(r.status))
            s.primary(I18n.t("Mark completed"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Mark completed?")).setMessage(I18n.t("Confirm the service has been done."))
                    .setPositiveButton(I18n.t("Yes, completed"), (x, y) -> go(mvm.complete(r.id), I18n.t("Marked completed")))
                    .setNegativeButton(I18n.t("Cancel"), null).show());
        s.show();
    }

    private void accept(HireRequest r) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.number("price", I18n.t("Agreed price (KSh, empty = your listed price)"), false));
        f.add(Forms.multi("note", I18n.t("Message to the farmer (optional)"), false));
        Forms.show(this, I18n.t("Accept request"), I18n.t("Accept"), f, v -> {
            HireAccept a = new HireAccept();
            String p = v.get("price").trim();
            try { a.quotedPrice = p.isEmpty() ? null : Double.valueOf(p); } catch (NumberFormatException x) { Ui.toast(this, I18n.t("Check the price")); return; }
            a.note = v.get("note");
            go(mvm.accept(r.id, a), I18n.t("Request accepted"));
        });
    }

    private void decline(HireRequest r) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("reason", I18n.t("Reason shown to the farmer"), true));
        Forms.show(this, I18n.t("Decline request"), I18n.t("Decline"), f, v -> {
            String reason = v.get("reason").trim();
            if (reason.length() < 5) Ui.toast(this, I18n.t("Please give a reason of at least 5 characters"));
            else go(mvm.decline(r.id, reason), I18n.t("Declined"));
        });
    }

    private void go(LiveData<Resource<HireRequest>> call, String ok) {
        Ui.watch(this, call, b.progress, r -> { Ui.toast(this, ok); load(); });
    }
}

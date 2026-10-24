package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.HireRequestForm;
import com.farmconnect.app.data.Models.HireService;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;

/** Farmers find transport, machinery hire, soil testing and more from verified companies, and send a request. */
public class ServiceHireActivity extends BaseListActivity {
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("Hire services"); }
    @Override protected boolean searchable() { return true; }
    @Override protected String fabText() { return I18n.t("My requests"); }
    @Override protected String emptyText() { return I18n.t("No services found"); }

    @Override protected String[][] chips() {
        String[][] c = new String[CompanyLabels.HIRE_CATEGORIES.length + 1][];
        c[0] = new String[]{"", "All"};
        for (int i = 0; i < CompanyLabels.HIRE_CATEGORIES.length; i++)
            c[i + 1] = new String[]{CompanyLabels.HIRE_CATEGORIES[i], CompanyLabels.HIRE_CATEGORY_NAMES[i]};
        return c;
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (mvm == null) return;
        Ui.watch(this, mvm.browse(chip, query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (HireService s : list) {
                rows.add(Row.of("🛠", s.name)
                        .sub(s.companyName + (s.companyCounty == null ? "" : " · " + s.companyCounty))
                        .meta(CompanyLabels.priceText(s.price, s.priceBasis))
                        .click(() -> detail(s)));
            }
            show(rows, list.size() + I18n.t(" services"));
        });
    }

    @Override protected void onFab() { startActivity(new Intent(this, MyServiceRequestsActivity.class)); }

    private void detail(HireService s) {
        Sheet.of(this, s.name)
                .badge(CompanyLabels.name(CompanyLabels.HIRE_CATEGORIES, CompanyLabels.HIRE_CATEGORY_NAMES, s.category), 0xFF2E7D32)
                .field(I18n.t("Company"), s.companyName + (s.companyCounty == null ? "" : ", " + s.companyCounty))
                .field(I18n.t("Counties served"), s.counties == null || s.counties.isEmpty() ? null : android.text.TextUtils.join(", ", s.counties))
                .note(s.description)
                .strong(I18n.t("Price"), CompanyLabels.priceText(s.price, s.priceBasis))
                .primary(I18n.t("Request this service"), () -> request(s))
                .show();
    }

    private void request(HireService s) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("details", I18n.t("What do you need?"), true));
        f.add(Forms.text("location", I18n.t("Where (village / farm location)"), true));
        f.add(Forms.text("date", I18n.t("Preferred date (YYYY-MM-DD, optional)"), false).value(CompanyLabels.inDays(3)));
        f.add(Forms.phone("phone", I18n.t("Your phone"), true));
        Forms.show(this, s.name + " · " + s.companyName, I18n.t("Send request"), f, v -> {
            HireRequestForm r = new HireRequestForm();
            r.details = v.get("details");
            r.location = v.get("location");
            String d = v.get("date").trim();
            r.preferredDate = d.isEmpty() ? null : d;
            r.phone = v.get("phone");
            Ui.watch(this, mvm.request(s.id, r), b.progress, x -> Ui.toast(this, I18n.t("Request sent. The company will respond.")));
        });
    }
}

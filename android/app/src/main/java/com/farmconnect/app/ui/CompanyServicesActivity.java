package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.HireService;
import com.farmconnect.app.data.Models.HireServiceForm;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;

/** Services the company offers farmers: transport, machinery hire, soil testing, irrigation, agronomy... */
public class CompanyServicesActivity extends BaseListActivity {
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("My services"); }
    @Override protected String fabText() { return I18n.t("Offer a service"); }
    @Override protected String emptyText() { return I18n.t("No services yet.\nOffer transport, machinery hire or advice to farmers."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (mvm == null) return;
        Ui.watch(this, mvm.services(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (HireService s : list) {
                rows.add(Row.of("🛠", s.name)
                        .sub(CompanyLabels.name(CompanyLabels.HIRE_CATEGORIES, CompanyLabels.HIRE_CATEGORY_NAMES, s.category) + " · " + CompanyLabels.priceText(s.price, s.priceBasis))
                        .meta(s.counties == null || s.counties.isEmpty() ? I18n.t("All your counties") : android.text.TextUtils.join(", ", s.counties))
                        .badge(s.active ? I18n.t("Active") : I18n.t("Hidden"), s.active ? 0xFF2E7D32 : 0xFF78909C)
                        .click(() -> detail(s)));
            }
            show(rows, list.size() + I18n.t(" services"));
        });
    }

    private void detail(HireService s) {
        Sheet sh = Sheet.of(this, s.name)
                .badge(s.active ? I18n.t("Visible to farmers") : I18n.t("Hidden"), s.active ? 0xFF2E7D32 : 0xFF78909C)
                .field(I18n.t("Category"), CompanyLabels.name(CompanyLabels.HIRE_CATEGORIES, CompanyLabels.HIRE_CATEGORY_NAMES, s.category))
                .field(I18n.t("Price"), CompanyLabels.priceText(s.price, s.priceBasis))
                .field(I18n.t("Counties"), s.counties == null || s.counties.isEmpty() ? null : android.text.TextUtils.join(", ", s.counties))
                .note(s.description)
                .secondary(I18n.t("Edit service"), () -> form(s));
        if (s.active) sh.danger(I18n.t("Hide from farmers"), () -> Ui.watch(this, mvm.hideService(s.id), b.progress, r -> { Ui.toast(this, I18n.t("Service hidden")); load(); }));
        sh.show();
    }

    @Override protected void onFab() { form(null); }

    private void form(HireService e) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", I18n.t("Service name"), true).value(e == null ? "" : e.name));
        f.add(Forms.choice("category", I18n.t("Category"), true, CompanyLabels.HIRE_CATEGORIES, CompanyLabels.HIRE_CATEGORY_NAMES).value(e == null ? "TRANSPORT" : e.category));
        f.add(Forms.number("price", I18n.t("Price (KSh, leave empty for price on request)"), false).value(e == null || e.price == null ? "" : CompanyLabels.qty(e.price)));
        f.add(Forms.text("basis", I18n.t("Price basis (per km, per acre, per visit)"), false).value(e == null ? "" : e.priceBasis));
        f.add(Forms.text("counties", I18n.t("Counties you serve (separate with commas, empty = all)"), false)
                .value(e == null || e.counties == null ? "" : android.text.TextUtils.join(", ", e.counties)));
        f.add(Forms.multi("description", I18n.t("Description"), false).value(e == null ? "" : e.description));
        f.add(Forms.choice("visible", I18n.t("Visible to farmers"), true, new String[]{"YES", "NO"}, new String[]{"Yes", "No (hidden)"}).value(e == null || e.active ? "YES" : "NO"));
        Forms.show(this, e == null ? I18n.t("Offer a service") : I18n.t("Edit service"), I18n.t("Save"), f, v -> {
            HireServiceForm r = new HireServiceForm();
            String price = v.get("price").trim();
            try { r.price = price.isEmpty() ? null : Double.valueOf(price); } catch (NumberFormatException x) { Ui.toast(this, I18n.t("Check the price")); return; }
            r.name = v.get("name");
            r.category = v.get("category");
            r.priceBasis = v.get("basis");
            r.description = v.get("description");
            r.active = "YES".equals(v.get("visible"));
            r.counties = new ArrayList<>();
            for (String c : v.get("counties").split(",")) if (!c.trim().isEmpty()) r.counties.add(c.trim());
            Ui.watch(this, mvm.saveService(e == null ? 0 : e.id, r), b.progress, x -> { Ui.toast(this, I18n.t("Service saved")); load(); });
        });
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.SupplyProductRequest;
import com.farmconnect.app.data.Models.SupplyProduct;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.List;

/** A verified supplier's product list: add, edit, hide. */
public class CompanyProductsActivity extends BaseListActivity {
    private SupplyViewModel svm;

    @Override protected String screenTitle() { return I18n.t("My products"); }
    @Override protected String fabText() { return I18n.t("Add product"); }
    @Override protected String emptyText() { return I18n.t("No products yet.\nAdd your first one."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        svm = new ViewModelProvider(this).get(SupplyViewModel.class);
    }

    @Override protected void load() {
        Ui.watch(this, svm.companyProducts(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (SupplyProduct p : list) {
                rows.add(Row.of("📦", p.name)
                        .sub(CompanyLabels.money(p.price) + " / " + p.unit + " · " + I18n.t("stock ") + p.stockQty)
                        .meta(CompanyLabels.name(CompanyLabels.SUPPLY_CATEGORIES, CompanyLabels.SUPPLY_CATEGORY_NAMES, p.category))
                        .badge(p.active ? I18n.t("Active") : I18n.t("Hidden"), p.active ? 0xFF2E7D32 : 0xFF78909C)
                        .click(() -> form(p)));
            }
            show(rows, list.size() + I18n.t(" products"));
        });
    }

    @Override protected void onFab() { form(null); }

    private void form(SupplyProduct e) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", I18n.t("Product name"), true).value(e == null ? "" : e.name));
        f.add(Forms.choice("category", I18n.t("Category"), true, CompanyLabels.SUPPLY_CATEGORIES, CompanyLabels.SUPPLY_CATEGORY_NAMES)
                .value(e == null ? "SEED" : e.category));
        f.add(Forms.text("unit", I18n.t("Unit (e.g. 50 kg bag, litre, each)"), true).value(e == null ? "" : e.unit));
        f.add(Forms.number("price", I18n.t("Price per unit (KSh)"), true).value(e == null ? "" : String.valueOf(e.price)));
        f.add(Forms.number("stock", I18n.t("Stock available"), true).value(e == null ? "0" : String.valueOf(e.stockQty)));
        f.add(Forms.number("min", I18n.t("Minimum order"), true).value(e == null ? "1" : String.valueOf(e.minOrderQty)));
        f.add(Forms.text("licence", I18n.t("Licence / batch reference (KEPHIS, PCPB...)"), false).value(e == null ? "" : e.licenceRef));
        f.add(Forms.multi("description", I18n.t("Description"), false).value(e == null ? "" : e.description));
        f.add(Forms.choice("visible", I18n.t("Visible to farmers"), true, new String[]{"YES", "NO"},
                new String[]{"Yes", "No (hidden)"}).value(e == null || e.active ? "YES" : "NO"));
        Forms.show(this, e == null ? I18n.t("Add product") : I18n.t("Edit product"), I18n.t("Save"), f, v -> {
            SupplyProductRequest r = new SupplyProductRequest();
            try {
                r.price = Double.parseDouble(v.get("price"));
                r.stockQty = Integer.parseInt(v.get("stock").trim());
                r.minOrderQty = Integer.parseInt(v.get("min").trim());
            } catch (NumberFormatException x) {
                Ui.toast(this, I18n.t("Check the price, stock and minimum order numbers"));
                return;
            }
            r.name = v.get("name");
            r.category = v.get("category");
            r.unit = v.get("unit");
            r.licenceRef = v.get("licence");
            r.description = v.get("description");
            r.active = "YES".equals(v.get("visible"));
            Ui.watch(this, svm.saveProduct(e == null ? 0 : e.id, r), b.progress, x -> {
                Ui.toast(this, I18n.t("Saved"));
                load();
            });
        });
    }
}

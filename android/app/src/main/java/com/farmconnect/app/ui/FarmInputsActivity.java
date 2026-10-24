package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.PlaceSupplyOrder;
import com.farmconnect.app.data.Models.SupplyLine;
import com.farmconnect.app.data.Models.SupplyProduct;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Farmers browse seed, fertiliser, feed and equipment from verified companies and order. One product per order here. */
public class FarmInputsActivity extends BaseListActivity {
    private SupplyViewModel svm;

    @Override protected String screenTitle() { return I18n.t("Farm inputs"); }
    @Override protected boolean searchable() { return true; }
    @Override protected String fabText() { return I18n.t("My orders"); }
    @Override protected String emptyText() { return I18n.t("No products found"); }

    @Override protected String[][] chips() {
        String[][] c = new String[CompanyLabels.SUPPLY_CATEGORIES.length + 1][];
        c[0] = new String[]{"", "All"};
        for (int i = 0; i < CompanyLabels.SUPPLY_CATEGORIES.length; i++)
            c[i + 1] = new String[]{CompanyLabels.SUPPLY_CATEGORIES[i], CompanyLabels.SUPPLY_CATEGORY_NAMES[i]};
        return c;
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        svm = new ViewModelProvider(this).get(SupplyViewModel.class);
    }

    @Override protected void load() {
        Ui.watch(this, svm.browse(chip, query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (SupplyProduct p : list) {
                rows.add(Row.of("🌾", p.name)
                        .sub(p.companyName + (p.county == null ? "" : " · " + p.county))
                        .meta(CompanyLabels.money(p.price) + " / " + p.unit + " · " + p.stockQty + I18n.t(" left")
                                + (p.licenceRef == null ? "" : " · " + p.licenceRef))
                        .click(() -> order(p)));
            }
            show(rows, list.size() + I18n.t(" products"));
        });
    }

    @Override protected void onFab() { startActivity(new Intent(this, MySupplyOrdersActivity.class)); }

    private void order(SupplyProduct p) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.number("qty", I18n.t("Quantity (") + p.unit + ", min " + p.minOrderQty + ")", true).value(String.valueOf(p.minOrderQty)));
        f.add(Forms.text("address", I18n.t("Delivery address"), true));
        f.add(Forms.phone("phone", I18n.t("Phone for delivery"), true));
        f.add(Forms.multi("note", I18n.t("Note (optional)"), false));
        Forms.show(this, p.name + " · " + CompanyLabels.money(p.price), I18n.t("Place order"), f, v -> {
            int qty;
            try { qty = Integer.parseInt(v.get("qty").trim()); } catch (NumberFormatException x) { Ui.toast(this, I18n.t("Enter a whole number")); return; }
            PlaceSupplyOrder r = new PlaceSupplyOrder();
            r.items = new ArrayList<>(Collections.singletonList(new SupplyLine(p.id, qty)));
            r.deliveryAddress = v.get("address");
            r.deliveryPhone = v.get("phone");
            r.deliveryNote = v.get("note");
            Ui.watch(this, svm.place(r), b.progress, o -> {
                Ui.toast(this, I18n.t("Order placed. You pay ") + p.companyName + I18n.t(" on delivery."));
                load();
            });
        });
    }
}

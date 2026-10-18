package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.util.Patterns;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.CountyAdminRequest;
import com.farmconnect.app.data.Models.UserSummary;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Super admin only: appoint county admins and choose the one county each of them manages.
 * The server refuses everyone else, so this screen is only a convenience.
 */
public class CountyAdminsActivity extends BaseListActivity {

    @Override protected String screenTitle() { return I18n.t("County admins"); }
    @Override protected String fabText() { return I18n.t("New county admin"); }
    @Override protected String emptyText() {
        return I18n.t("No county admins yet.\nCreate one and choose the county they manage.");
    }

    @Override protected void load() {
        Ui.watch(this, vm.countyAdmins(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (UserSummary u : list) {
                String county = u.assignedCounty == null || u.assignedCounty.isEmpty() ? null : u.assignedCounty;
                Row r = Row.of("🗺", u.name)
                        .sub(u.email + (u.phone == null || u.phone.isEmpty() ? "" : "  ·  " + u.phone))
                        .meta(county == null ? I18n.t("⚠ No county assigned. They cannot see anything yet.")
                                : I18n.t("Manages ") + county + I18n.t(" county only") + (u.enabled ? "" : I18n.t("  ·  ⛔ Suspended")))
                        .click(() -> pickCounty(u));
                r.badge(county == null ? I18n.t("No county") : county, county == null ? 0xFFC62828 : 0xFF2E7D32);
                rows.add(r);
            }
            show(rows, list.size() + (list.size() == 1 ? I18n.t(" county admin") : I18n.t(" county admins")));
        });
    }

    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", I18n.t("Full name"), true));
        f.add(Forms.email("email", I18n.t("Email (they log in with this)"), true));
        f.add(Forms.phone("phone", I18n.t("Phone"), false));
        f.add(Forms.password("password", I18n.t("Password (at least 8 characters)"), true));
        f.add(Forms.choice("county", I18n.t("County they will manage"), true, Labels.COUNTIES, Labels.COUNTIES));
        Forms.show(this, I18n.t("New county admin"), I18n.t("Create account"), f, v -> {
            String email = v.get("email");
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { Ui.toast(this, I18n.t("Enter a valid email address")); return; }
            if (v.get("password").length() < 8) { Ui.toast(this, I18n.t("The password needs at least 8 characters.")); return; }
            Ui.watch(this, vm.createCountyAdmin(new CountyAdminRequest(v.get("name"), email, v.get("phone"), v.get("password"), v.get("county"))),
                    b.progress, u -> {
                        Ui.toast(this, I18n.t("County admin created for ") + u.assignedCounty);
                        load();
                    });
        });
    }

    /** Tap a county admin to move them to another county. */
    private void pickCounty(UserSummary u) {
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("County for ") + u.name)
                .setItems(Labels.COUNTIES, (d, which) -> {
                    String county = Labels.COUNTIES[which];
                    Ui.watch(this, vm.setCountyAdminCounty(u.id, county), b.progress, x -> {
                        Ui.toast(this, I18n.t("Now manages ") + county);
                        load();
                    });
                })
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }
}

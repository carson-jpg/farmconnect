package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.ServiceProvider;
import com.farmconnect.app.data.Models.ServiceRequest;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/** County directory of agro-dealers, vets, extension officers, transporters, buyers, finance and more. */
public class ServicesActivity extends BaseListActivity {
    @Override protected String screenTitle() { return I18n.t("Service directory"); }
    @Override protected boolean searchable() { return true; }
    @Override protected String searchHint() { return I18n.t("Search seeds, vets, transport..."); }
    @Override protected String fabText() { return Labels.isStaff() ? I18n.t("Add provider") : null; }
    @Override protected String emptyText() { return I18n.t("No providers listed yet.\nThe county office keeps this directory up to date."); }

    @Override protected String[][] chips() {
        String[][] c = new String[Labels.SERVICE_CATS.length + 1][];
        c[0] = new String[]{"", "All"};
        for (int i = 0; i < Labels.SERVICE_CATS.length; i++) c[i + 1] = new String[]{Labels.SERVICE_CATS[i], Labels.SERVICE_NAMES[i]};
        return c;
    }

    @Override protected void load() {
        Ui.watch(this, vm.services(chip, query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (ServiceProvider s : list) {
                String where = (s.location == null || s.location.isEmpty() ? "" : "📍 " + s.location)
                        + (s.subCounty == null || s.subCounty.isEmpty() ? "" : (s.location == null || s.location.isEmpty() ? "📍 " : " · ") + s.subCounty);
                Row r = Row.of(Labels.serviceIcon(s.category), s.name)
                        .sub(s.services != null && !s.services.isEmpty() ? s.services : s.description)
                        .meta(where)
                        .badge(Labels.name(Labels.SERVICE_CATS, Labels.SERVICE_NAMES, s.category))
                        .click(() -> details(s));
                if (s.phone != null && !s.phone.isEmpty()) r.action(I18n.t("📞 Call"), () -> call(s.phone));
                rows.add(r);
            }
            show(rows, list.size() + (list.size() == 1 ? I18n.t(" provider") : I18n.t(" providers")));
        });
    }

    private void call(String phone) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.replaceAll("[^0-9+]", ""))));
    }

    private void details(ServiceProvider s) {
        StringBuilder m = new StringBuilder();
        if (s.description != null && !s.description.isEmpty()) m.append(s.description).append("\n\n");
        if (s.services != null && !s.services.isEmpty()) m.append(I18n.t("Services: ")).append(s.services).append("\n");
        if (s.phone != null && !s.phone.isEmpty()) m.append(I18n.t("Phone: ")).append(s.phone).append("\n");
        if (s.email != null && !s.email.isEmpty()) m.append(I18n.t("Email: ")).append(s.email).append("\n");
        if (s.location != null && !s.location.isEmpty()) m.append(I18n.t("Location: ")).append(s.location).append("\n");
        if (s.subCounty != null && !s.subCounty.isEmpty()) m.append(I18n.t("Sub-county: ")).append(s.subCounty);
        AlertDialog.Builder d = new AlertDialog.Builder(this).setTitle(Labels.serviceIcon(s.category) + "  " + s.name)
                .setMessage(m.toString().trim()).setNegativeButton(I18n.t("Close"), null);
        if (s.phone != null && !s.phone.isEmpty()) d.setPositiveButton(I18n.t("Call"), (x, y) -> call(s.phone));
        if (Labels.isStaff()) d.setNeutralButton(I18n.t("Delete"), (x, y) -> Ui.watch(this, vm.deleteService(s.id), b.progress, r -> {
            Ui.toast(this, I18n.t("Removed from the directory"));
            load();
        }));
        d.show();
    }

    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", I18n.t("Business / office name"), true));
        f.add(Forms.choice("category", I18n.t("Category"), true, Labels.SERVICE_CATS, Labels.SERVICE_NAMES));
        f.add(Forms.text("services", I18n.t("What they offer (e.g. seeds, fertiliser, vaccines)"), false));
        f.add(Forms.multi("description", I18n.t("About"), false));
        f.add(Forms.phone("phone", I18n.t("Phone"), false));
        f.add(Forms.email("email", I18n.t("Email"), false));
        f.add(Forms.choice("subCounty", I18n.t("Sub-county"), false, Labels.SUB_COUNTIES, Labels.SUB_COUNTIES));
        f.add(Forms.text("location", I18n.t("Town / location"), false));
        Forms.show(this, I18n.t("Add to directory"), I18n.t("Add"), f, v -> {
            ServiceRequest r = new ServiceRequest();
            r.name = v.get("name");
            r.category = v.get("category");
            r.services = v.get("services");
            r.description = v.get("description");
            r.phone = v.get("phone");
            r.email = v.get("email");
            r.subCounty = v.get("subCounty");
            r.location = v.get("location");
            Ui.watch(this, vm.createService(r), b.progress, x -> {
                Ui.toast(this, I18n.t("Added"));
                load();
            });
        });
    }
}

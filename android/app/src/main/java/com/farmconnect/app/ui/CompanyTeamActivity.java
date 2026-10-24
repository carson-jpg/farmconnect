package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.TeamMember;
import com.farmconnect.app.data.Models.TeamMemberForm;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;

/** The people who work at the company. Contact records only: they do not get a login. */
public class CompanyTeamActivity extends BaseListActivity {
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("Team"); }
    @Override protected String fabText() { return I18n.t("Add member"); }
    @Override protected String emptyText() { return I18n.t("No team members yet.\nAdd your managers, sales staff and drivers."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
    }

    @Override protected void load() {
        if (mvm == null) return;
        Ui.watch(this, mvm.team(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (TeamMember t : list) {
                Row r = Row.of("👤", t.name)
                        .sub(CompanyLabels.name(CompanyLabels.MEMBER_ROLES, CompanyLabels.MEMBER_ROLE_NAMES, t.role) + (t.phone == null ? "" : " · " + t.phone))
                        .meta(t.email == null ? "" : t.email)
                        .click(() -> detail(t));
                if (!t.active) r.badge(I18n.t("Inactive"), 0xFF78909C);
                rows.add(r);
            }
            show(rows, list.size() + I18n.t(" people"));
        });
    }

    private void detail(TeamMember t) {
        Sheet.of(this, t.name)
                .badge(CompanyLabels.name(CompanyLabels.MEMBER_ROLES, CompanyLabels.MEMBER_ROLE_NAMES, t.role), 0xFF2E7D32)
                .field(I18n.t("Phone"), t.phone)
                .field(I18n.t("Email"), t.email)
                .field(I18n.t("Status"), t.active ? I18n.t("Currently working here") : I18n.t("Inactive"))
                .secondary(I18n.t("Edit"), () -> form(t))
                .danger(I18n.t("Remove from team"), () -> new AlertDialog.Builder(this)
                        .setTitle(I18n.t("Remove ") + t.name + "?")
                        .setPositiveButton(I18n.t("Remove"), (x, y) -> Ui.watch(this, mvm.deleteMember(t.id), b.progress, r -> { Ui.toast(this, I18n.t("Removed")); load(); }))
                        .setNegativeButton(I18n.t("Cancel"), null).show())
                .show();
    }

    @Override protected void onFab() { form(null); }

    private void form(TeamMember e) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", I18n.t("Name"), true).value(e == null ? "" : e.name));
        f.add(Forms.choice("role", I18n.t("Role"), true, CompanyLabels.MEMBER_ROLES, CompanyLabels.MEMBER_ROLE_NAMES).value(e == null ? "MANAGER" : e.role));
        f.add(Forms.phone("phone", I18n.t("Phone"), false).value(e == null ? "" : e.phone));
        f.add(Forms.email("email", I18n.t("Email"), false).value(e == null ? "" : e.email));
        f.add(Forms.choice("active", I18n.t("Currently working here"), true, new String[]{"YES", "NO"}, new String[]{"Yes", "No"}).value(e == null || e.active ? "YES" : "NO"));
        Forms.show(this, e == null ? I18n.t("Add team member") : I18n.t("Edit team member"), I18n.t("Save"), f, v -> {
            TeamMemberForm r = new TeamMemberForm();
            r.name = v.get("name");
            r.role = v.get("role");
            r.phone = v.get("phone");
            r.email = v.get("email");
            r.active = "YES".equals(v.get("active"));
            Ui.watch(this, mvm.saveMember(e == null ? 0 : e.id, r), b.progress, x -> { Ui.toast(this, I18n.t("Saved")); load(); });
        });
    }
}

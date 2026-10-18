package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.farmconnect.app.data.Models.AuditEntry;
import com.farmconnect.app.data.Models.LockInfo;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;
import java.util.ArrayList;
import java.util.List;

/** Super admin only: what is happening on the platform (sign-ins, blocked attempts, staff actions) and lockouts to clear. */
public class SecurityActivity extends BaseListActivity {

    @Override protected String screenTitle() { return I18n.t("Security"); }
    @Override protected String emptyText() { return I18n.t("Nothing logged yet."); }
    @Override protected boolean searchable() { return true; }
    @Override protected String searchHint() { return I18n.t("Search an email, address or action..."); }
    @Override protected String headerActionLabel() { return I18n.t("My password"); }
    @Override protected void onHeaderAction() { changePasswordDialog(this, vm); }

    @Override protected String[][] chips() {
        return new String[][]{{"", "Everything"}, {"LOCKS", "Lockouts"}, {"LOGIN_FAILED", "Wrong passwords"}, {"LOGIN_BLOCKED", "Blocked"},
                {"ACCESS_DENIED", "Access denied"}, {"LOGIN_OK", "Sign-ins"}, {"STAFF_ACTION", "Staff actions"}};
    }

    private static String label(String action) {
        switch (action == null ? "" : action) {
            case "LOGIN_OK": return "Signed in";
            case "LOGIN_FAILED": return "Wrong password";
            case "LOGIN_BLOCKED": return "Blocked (locked out)";
            case "LOGIN_REFUSED": return "Suspended account tried to sign in";
            case "ACCESS_DENIED": return "Access denied";
            case "STAFF_ACTION": return "Staff action";
            case "STAFF_ACTION_FAILED": return "Staff action failed";
            case "PASSWORD_RESET": return "Password reset";
            case "PASSWORD_CHANGED": return "Password changed";
            case "FORCE_LOGOUT": return "Signed out everywhere";
            case "ADMIN_PASSWORD_RESET": return "Temporary password issued";
            case "UNLOCK": return "Lockout cleared";
            default: return action == null ? "" : action;
        }
    }

    @Override protected void load() {
        if (!"ADMIN".equals(Session.role())) { finish(); return; }      // the server refuses everyone else too
        if ("LOCKS".equals(chip)) { loadLocks(); return; }
        Ui.watch(this, vm.securityAudit(chip.isEmpty() ? null : chip, query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (AuditEntry a : list) {
                String who = a.actorEmail == null ? I18n.t("(not signed in)") : a.actorEmail;
                StringBuilder sub = new StringBuilder();
                if (a.target != null && !a.target.isEmpty()) sub.append(a.target);
                if (a.detail != null && !a.detail.isEmpty()) sub.append(sub.length() > 0 ? "\n" : "").append(a.detail);
                rows.add(Row.of(a.success ? "✅" : "⛔", who)
                        .sub(sub.length() == 0 ? null : sub.toString())
                        .meta(Ui.dateTime(a.at) + (a.ip == null ? "" : "  ·  " + a.ip))
                        .badge(I18n.t(label(a.action)), a.success ? 0xFF2E7D32 : 0xFFC62828));
            }
            show(rows, list.size() + I18n.t(" events"));
        });
    }

    private void loadLocks() {
        Ui.watch(this, vm.securityLocks(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (LockInfo l : list) {
                boolean address = "ADDRESS".equals(l.kind);
                String subject = l.subject.split(" from ")[0];
                rows.add(Row.of("🔒", l.subject)
                        .sub(address ? I18n.t("Address locked") : I18n.t("Account locked"))
                        .meta(I18n.t("Until ") + Ui.dateTime(l.until))
                        .action(I18n.t("Unlock"), () -> Ui.watch(this, vm.unlock(address ? null : subject, address ? subject : null), b.progress, c -> {
                            Ui.toast(this, I18n.t("Unlocked"));
                            load();
                        })));
            }
            show(rows, list.isEmpty() ? I18n.t("Nobody is locked out") : list.size() + I18n.t(" active lockouts"));
        });
    }

    /** Change my own password. Other devices are signed out; this device keeps working with a fresh token. */
    static void changePasswordDialog(AppCompatActivity a, CommunityViewModel vm) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.password("current", I18n.t("Current password"), true));
        f.add(Forms.password("next", I18n.t("New password (8+ characters, a letter and a number)"), true));
        Forms.show(a, I18n.t("Change my password"), I18n.t("Change password"), f, v ->
                Ui.watch(a, vm.changePassword(v.get("current"), v.get("next")), null, r -> {
                    Session.save(r);   // the old token was just ended
                    Ui.toast(a, I18n.t("Password changed. Other devices were signed out."));
                }));
    }

    /** Shows a temporary password once, after the super admin reset someone's password. */
    static void showTempPassword(AppCompatActivity a, String email, String temp) {
        new AlertDialog.Builder(a).setTitle(I18n.t("Temporary password"))
                .setMessage(email + "\n\n" + temp + "\n\n" + I18n.t("Give this to them privately. It is shown only once."))
                .setPositiveButton(I18n.t("Done"), null).show();
    }
}

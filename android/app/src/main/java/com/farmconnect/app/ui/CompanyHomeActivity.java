package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.CompanyRequest;
import com.farmconnect.app.data.Models.CompanyView;
import com.farmconnect.app.data.Models.SupplySummary;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CompanyViewModel;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.List;

/** Company portal home: verification progress, key numbers and shortcuts. The super admin verifies every company. */
public class CompanyHomeActivity extends BaseListActivity {
    private CompanyViewModel cvm;
    private CompanyView company;
    private boolean askedDetails;
    private SupplySummary summary;
    private boolean summaryAsked;

    @Override protected String screenTitle() { return I18n.t("Company portal"); }
    @Override protected String headerActionLabel() { return I18n.t("Log out"); }
    @Override protected void onHeaderAction() { Ui.logout(this); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b.btnBack.setVisibility(View.GONE);
        cvm = new ViewModelProvider(this).get(CompanyViewModel.class);
        com.farmconnect.app.push.Push.sync(this);
    }

    @Override protected void load() {
        if (cvm == null) return;
        summaryAsked = false;
        cvm.mine().observe(this, r -> {
            b.progress.setVisibility(r.status == Resource.Status.LOADING ? View.VISIBLE : View.GONE);
            if (r.status == Resource.Status.SUCCESS) { company = r.data; showHome(); }
            else if (r.status == Resource.Status.ERROR) {
                if ("NO_COMPANY".equals(r.message)) { company = null; showSetup(); }
                else Ui.toast(this, r.message);
            }
        });
    }

    private void showSetup() {
        b.topArea.removeAllViews();
        UiKit.hero(this, b.topArea, I18n.t("Welcome to FarmConnect"), I18n.t("Enter your business details. The FarmConnect super admin will verify them before you can trade."),
                I18n.t("Step 2 of 2"), 0xFFF9A825);
        List<Row> rows = new ArrayList<>();
        rows.add(Row.of("🏢", I18n.t("Register your company")).sub(I18n.t("Name, registration number, KRA PIN and contacts")).click(() -> companyForm(null)));
        show(rows, "");
        if (!askedDetails) { askedDetails = true; companyForm(null); }
    }

    private String statusMessage(CompanyView c) {
        switch (c.status) {
            case "PENDING_VERIFICATION": return hasDocs(c) ? I18n.t("Your documents are in. The super admin will review your company soon.") : I18n.t("Upload your registration and KRA PIN certificates so the super admin can review you.");
            case "UNDER_REVIEW": return I18n.t("The super admin is reviewing your company now.");
            case "VERIFIED": return I18n.t("Your company is verified and can trade on FarmConnect.");
            case "REJECTED": return I18n.t("Not verified: ") + (c.rejectionReason == null ? "" : c.rejectionReason) + I18n.t(". Fix it and save your profile to resubmit.");
            default: return I18n.t("Suspended: ") + (c.suspensionReason == null ? "" : c.suspensionReason) + I18n.t(". Contact FarmConnect support.");
        }
    }

    private static boolean hasDocs(CompanyView c) {
        boolean reg = false, kra = false;
        if (c.documents != null) for (com.farmconnect.app.data.Models.CompanyDocument d : c.documents) {
            if ("REGISTRATION_CERT".equals(d.type)) reg = true;
            if ("KRA_PIN_CERT".equals(d.type)) kra = true;
        }
        return reg && kra;
    }

    /** Registered > Documents > Review > Verified, with the current step marked. */
    private void progress(CompanyView c) {
        boolean docs = hasDocs(c), verified = "VERIFIED".equals(c.status) || "SUSPENDED".equals(c.status);
        int[] st = {2,
                docs ? 2 : 1,
                verified ? 2 : "REJECTED".equals(c.status) ? 3 : docs ? 1 : 0,
                "VERIFIED".equals(c.status) ? 2 : "SUSPENDED".equals(c.status) ? 3 : 0};
        UiKit.stepper(this, b.topArea, new String[]{I18n.t("Registered"), I18n.t("Documents"), I18n.t("Review"), I18n.t("Verified")}, st);
    }

    private void showHome() {
        CompanyView c = company;
        LinearLayout top = b.topArea;
        top.removeAllViews();
        top.setPadding(Cards.dp(this, 12), Cards.dp(this, 6), Cards.dp(this, 12), 0);
        UiKit.hero(this, top, c.name, statusMessage(c), CompanyLabels.status(c.status), CompanyLabels.statusColor(c.status));
        if (!"VERIFIED".equals(c.status)) progress(c);
        if (c.verified && c.canSell) {
            if (summary != null) {
                LinearLayout r1 = Cards.statRow(this, top);
                r1.addView(Cards.stat(this, String.valueOf(summary.newOrders), I18n.t("New orders")));
                r1.addView(Cards.stat(this, String.valueOf(summary.inProgress), I18n.t("In progress")));
                r1.addView(Cards.stat(this, String.valueOf(summary.delivered), I18n.t("Delivered")));
                LinearLayout r2 = Cards.statRow(this, top);
                r2.addView(Cards.stat(this, Ui.kes(summary.grossDelivered), I18n.t("Sales")));
                r2.addView(Cards.stat(this, Ui.kes(summary.netDelivered), I18n.t("You earn")));
                r2.addView(Cards.stat(this, Ui.kes(summary.netUnpaid), I18n.t("To collect")));
            }
            if (!summaryAsked) {
                summaryAsked = true;
                new ViewModelProvider(this).get(SupplyViewModel.class).summary().observe(this, r -> {
                    if (r.status == Resource.Status.SUCCESS) { summary = r.data; showHome(); }
                });
            }
        }

        List<Row> rows = new ArrayList<>();
        if (c.verified && c.canSell) {
            int fresh = summary == null ? 0 : summary.newOrders;
            rows.add(Row.of("📦", I18n.t("Products")).sub(summary == null ? "" : summary.activeProducts + I18n.t(" active") + (summary.lowStock > 0 ? " · " + summary.lowStock + I18n.t(" low on stock") : ""))
                    .click(() -> startActivity(new Intent(this, CompanyProductsActivity.class))));
            Row orders = Row.of("🧾", I18n.t("Orders")).sub(summary == null ? "" : fresh + I18n.t(" new · ") + summary.inProgress + I18n.t(" in progress"))
                    .click(() -> startActivity(new Intent(this, CompanyOrdersActivity.class)));
            if (fresh > 0) orders.badge(String.valueOf(fresh), 0xFFE53935);
            rows.add(orders);
            rows.add(Row.of("🛠", I18n.t("Services")).sub(I18n.t("Transport, machinery hire, soil testing..."))
                    .click(() -> startActivity(new Intent(this, CompanyServicesActivity.class))));
            rows.add(Row.of("📨", I18n.t("Service requests")).sub(I18n.t("Farmers asking for your services"))
                    .click(() -> startActivity(new Intent(this, CompanyServiceRequestsActivity.class))));
            String payoutState = c.payoutAccount == null ? I18n.t("Not set. Add where to send your money.")
                    : c.payoutApproved ? I18n.t("Approved: ") + c.payoutAccount : I18n.t("Waiting for super admin approval: ") + c.payoutAccount;
            Row payout = Row.of("🏦", I18n.t("Payout account")).sub(payoutState).click(() -> payoutForm(c));
            if (c.payoutAccount == null || !c.payoutApproved)
                payout.badge(c.payoutAccount == null ? I18n.t("Set up") : I18n.t("Pending"), c.payoutAccount == null ? 0xFFE53935 : 0xFFF9A825);
            rows.add(payout);
            rows.add(Row.of("💸", I18n.t("Payouts")).sub(I18n.t("What FarmConnect sent to your account"))
                    .click(() -> startActivity(new Intent(this, CompanyPayoutsActivity.class))));
        }
        if (c.verified && c.canBuy) {
            rows.add(Row.of("🌽", I18n.t("Produce requests")).sub(I18n.t("Post what you want to buy and get quotations from farmers"))
                    .click(() -> startActivity(new Intent(this, CompanyRequestsActivity.class))));
            rows.add(Row.of("📑", I18n.t("Purchase orders")).sub(I18n.t("Orders from accepted quotations"))
                    .click(() -> startActivity(new Intent(this, CompanyBulkOrdersActivity.class))));
        }
        if (!c.verified)
            rows.add(Row.of("🔒", I18n.t("Products, orders and requests")).sub(I18n.t("Unlock after the super admin verifies your company"))
                    .click(() -> Ui.toast(this, I18n.t("Your company is not verified yet"))));
        rows.add(Row.of("👥", I18n.t("Team")).sub(I18n.t("Managers, sales staff and drivers"))
                .click(() -> startActivity(new Intent(this, CompanyTeamActivity.class))));
        rows.add(Row.of("📝", I18n.t("Company profile")).sub(CompanyLabels.name(CompanyLabels.CATEGORIES, CompanyLabels.CATEGORY_NAMES, c.category))
                .click(() -> companyForm(c)));
        int n = c.documents == null ? 0 : c.documents.size();
        rows.add(Row.of("📄", I18n.t("Documents")).sub(n + (n == 1 ? I18n.t(" document uploaded") : I18n.t(" documents uploaded")))
                .click(() -> startActivity(new Intent(this, CompanyDocsActivity.class))));
        rows.add(Row.of("🔔", I18n.t("Alerts")).click(() -> startActivity(new Intent(this, NotificationsActivity.class))));
        rows.add(Row.of("💬", I18n.t("Messages")).click(() -> startActivity(new Intent(this, MessagesActivity.class))));
        rows.add(Row.of("🔑", I18n.t("Change password")).click(() -> SecurityActivity.changePasswordDialog(this, vm)));
        rows.add(Row.of("🌐", I18n.t("Language")).click(() -> I18n.chooseLanguage(this)));
        show(rows, "");
    }

    /** Where FarmConnect sends this company's money. The super admin must approve it (again after every change). */
    private void payoutForm(CompanyView c) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("type", I18n.t("Send my money to"), true, CompanyLabels.PAYOUT_TYPES, CompanyLabels.PAYOUT_TYPE_NAMES)
                .value(c.payoutType == null ? "MPESA_PHONE" : c.payoutType));
        f.add(Forms.text("account", I18n.t("Phone, paybill or till number"), true).value(c.payoutAccount));
        f.add(Forms.text("reference", I18n.t("Account number (paybill only)"), false).value(c.payoutReference));
        Forms.show(this, I18n.t("Payout account"), I18n.t("Save"), f, v -> {
            com.farmconnect.app.data.Models.PayoutAccountRequest r = new com.farmconnect.app.data.Models.PayoutAccountRequest();
            r.type = v.get("type");
            r.account = v.get("account");
            r.reference = v.get("reference");
            Ui.watch(this, cvm.setPayoutAccount(r), b.progress, res -> {
                Ui.toast(this, I18n.t("Saved. Waiting for the super admin to approve it."));
                load();
            });
        });
    }

    /** Register (existing == null) or edit the company. Name, registration number and KRA PIN are locked by the server once verified. */
    private void companyForm(CompanyView e) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", I18n.t("Registered company name"), true).value(e == null ? "" : e.name));
        f.add(Forms.text("reg", I18n.t("Business registration number"), true).value(e == null ? "" : e.businessRegNumber));
        f.add(Forms.text("kra", I18n.t("KRA PIN (e.g. A123456789B)"), true).value(e == null ? "" : e.kraPin));
        f.add(Forms.choice("category", I18n.t("Business category"), true, CompanyLabels.CATEGORIES, CompanyLabels.CATEGORY_NAMES)
                .value(e == null ? "AGRI_SUPPLIER" : e.category));
        f.add(Forms.choice("role", I18n.t("The company"), true, CompanyLabels.ROLES, CompanyLabels.ROLE_NAMES)
                .value(e == null ? "SELL" : (e.canBuy && e.canSell ? "BOTH" : e.canBuy ? "BUY" : "SELL")));
        f.add(Forms.text("repName", I18n.t("Authorised representative"), true).value(e == null ? com.farmconnect.app.data.Session.name() : e.repName));
        f.add(Forms.phone("repPhone", I18n.t("Representative phone"), true).value(e == null ? "" : e.repPhone));
        f.add(Forms.email("repEmail", I18n.t("Representative email"), false).value(e == null ? "" : e.repEmail));
        f.add(Forms.text("address", I18n.t("Physical address"), true).value(e == null ? "" : e.address));
        f.add(Forms.choice("county", I18n.t("County"), true, Labels.COUNTIES, Labels.COUNTIES).value(e == null ? "" : e.county));
        f.add(Forms.text("operating", I18n.t("Counties you operate in (separate with commas)"), false)
                .value(e == null || e.operatingCounties == null ? "" : android.text.TextUtils.join(", ", e.operatingCounties)));
        f.add(Forms.multi("description", I18n.t("About the company"), false).value(e == null ? "" : e.description));
        Forms.show(this, e == null ? I18n.t("Register company") : I18n.t("Company profile"),
                e == null ? I18n.t("Register") : I18n.t("Save"), f, v -> {
                    CompanyRequest r = new CompanyRequest();
                    r.name = v.get("name");
                    r.businessRegNumber = v.get("reg");
                    r.kraPin = v.get("kra").toUpperCase();
                    r.category = v.get("category");
                    String role = v.get("role");
                    r.canBuy = "BOTH".equals(role) || "BUY".equals(role);
                    r.canSell = "BOTH".equals(role) || "SELL".equals(role);
                    r.repName = v.get("repName");
                    r.repPhone = v.get("repPhone");
                    r.repEmail = v.get("repEmail");
                    r.address = v.get("address");
                    r.county = v.get("county");
                    r.description = v.get("description");
                    r.operatingCounties = new ArrayList<>();
                    for (String c : v.get("operating").split(",")) if (!c.trim().isEmpty()) r.operatingCounties.add(c.trim());
                    Ui.watch(this, e == null ? cvm.register(r) : cvm.update(r), b.progress, res -> {
                        Ui.toast(this, e == null ? I18n.t("Company registered. Now upload your documents.") : I18n.t("Saved"));
                        load();
                    });
                });
    }
}

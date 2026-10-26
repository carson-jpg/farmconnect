package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkOrder;
import com.farmconnect.app.data.Models.BulkRequest;
import com.farmconnect.app.data.Models.CompanyRequest;
import com.farmconnect.app.data.Models.HireRequest;
import com.farmconnect.app.data.Models.SupplyItem;
import com.farmconnect.app.data.Models.SupplyOrder;
import com.farmconnect.app.data.Models.CompanyView;
import com.farmconnect.app.data.Models.SupplySummary;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BulkViewModel;
import com.farmconnect.app.vm.CompanyViewModel;
import com.farmconnect.app.vm.MarketViewModel;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Company portal home: verification progress, key numbers and shortcuts. The super admin verifies every company. */
public class CompanyHomeActivity extends BaseListActivity {
    private CompanyViewModel cvm;
    private CompanyView company;
    private boolean askedDetails;
    private SupplySummary summary;
    private boolean summaryAsked;
    private List<SupplyOrder> orderList = new ArrayList<>();
    private List<HireRequest> serviceList = new ArrayList<>();
    private List<BulkRequest> requestList = new ArrayList<>();
    private List<BulkOrder> poList = new ArrayList<>();
    private LinearLayout insights;

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
        insights = new LinearLayout(this);
        insights.setOrientation(LinearLayout.VERTICAL);
        top.addView(insights, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (c.verified) {
            renderInsights();
            if (!summaryAsked) { summaryAsked = true; loadInsights(c); }
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
        if (c.verified && "TRANSPORT".equals(c.category))
            rows.add(Row.of("🚚", I18n.t("Delivery jobs")).sub(I18n.t("Requests from buyers, collections and deliveries"))
                    .click(() -> startActivity(new Intent(this, DeliveryActivity.class))));
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

    /** Fetches the lists the numbers and charts are built from. Each arrival redraws the section. */
    private void loadInsights(CompanyView c) {
        if (c.canSell) {
            new ViewModelProvider(this).get(SupplyViewModel.class).summary().observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS) { summary = r.data; renderInsights(); }
            });
            new ViewModelProvider(this).get(SupplyViewModel.class).companyOrders("").observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS && r.data != null) { orderList = r.data; renderInsights(); }
            });
            new ViewModelProvider(this).get(MarketViewModel.class).serviceRequests("").observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS && r.data != null) { serviceList = r.data; renderInsights(); }
            });
        }
        if (c.canBuy) {
            BulkViewModel bvm = new ViewModelProvider(this).get(BulkViewModel.class);
            bvm.companyRequests().observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS && r.data != null) { requestList = r.data; renderInsights(); }
            });
            bvm.companyOrders().observe(this, r -> {
                if (r.status == Resource.Status.SUCCESS && r.data != null) { poList = r.data; renderInsights(); }
            });
        }
    }

    private void open(Class<?> c) { startActivity(new Intent(this, c)); }

    /** Selling and Buying blocks like the web overview: numbers first, then charts. */
    private void renderInsights() {
        CompanyView c = company;
        if (insights == null || c == null || !c.verified) return;
        insights.removeAllViews();

        if (c.canSell && summary != null) {
            SupplySummary s = summary;
            UiKit.section(this, insights, I18n.t("Selling"));
            List<View> t = new ArrayList<>();
            t.add(UiKit.statCard(this, "📥", I18n.t("New orders"), String.valueOf(s.newOrders), I18n.t("waiting for you"), s.newOrders > 0 ? 0xFFF9A825 : 0xFF2E7D32, v -> open(CompanyOrdersActivity.class)));
            t.add(UiKit.statCard(this, "🚚", I18n.t("In progress"), String.valueOf(s.inProgress), I18n.t("confirmed or dispatched"), 0xFF1E88E5, v -> open(CompanyOrdersActivity.class)));
            t.add(UiKit.statCard(this, "✅", I18n.t("Delivered"), String.valueOf(s.delivered), I18n.t("orders"), 0xFF2E7D32, v -> open(CompanyOrdersActivity.class)));
            t.add(UiKit.statCard(this, "📦", I18n.t("Active products"), String.valueOf(s.activeProducts), s.lowStock + I18n.t(" low on stock"), s.lowStock > 0 ? 0xFFE53935 : 0xFF2E7D32, v -> open(CompanyProductsActivity.class)));
            t.add(UiKit.statCard(this, "📈", I18n.t("Sales delivered"), Ui.kes(s.grossDelivered), I18n.t("before fees"), 0xFF2E7D32, null));
            t.add(UiKit.statCard(this, "🏷", I18n.t("FarmConnect fees"), Ui.kes(s.feesDelivered), I18n.t("on delivered orders"), 0xFF8E24AA, null));
            t.add(UiKit.statCard(this, "💰", I18n.t("You earn"), Ui.kes(s.netDelivered), I18n.t("after fees"), 0xFF2E7D32, v -> open(CompanyPayoutsActivity.class)));
            t.add(UiKit.statCard(this, "⏳", I18n.t("Still to collect"), Ui.kes(s.netUnpaid), I18n.t("delivered, not paid"), s.netUnpaid > 0 ? 0xFFF9A825 : 0xFF2E7D32, null));
            UiKit.grid(this, insights, t);

            LinkedHashMap<String, Double> sales = Insights.months(6), cnt = Insights.months(6);
            Map<String, Long> units = new LinkedHashMap<>();
            for (SupplyOrder o : orderList) {
                boolean live = !"CANCELLED".equals(o.status) && !"REJECTED".equals(o.status);
                if ("DELIVERED".equals(o.status)) Insights.add(sales, o.deliveredAt != null ? o.deliveredAt : o.createdAt, o.total);
                if (live) {
                    Insights.add(cnt, o.createdAt, 1);
                    if (o.items != null) for (SupplyItem i : o.items) units.put(i.name, (units.containsKey(i.name) ? units.get(i.name) : 0L) + i.quantity);
                }
            }
            LinearLayout a = UiKit.chartCard(this, insights, I18n.t("Sales per month (KSh)"));
            a.addView(Charts.columns(this, sales, 0xFF2E7D32));
            LinearLayout b2 = UiKit.chartCard(this, insights, I18n.t("Orders per month"));
            b2.addView(Charts.columns(this, cnt, 0xFF1E88E5));
            Charts.donut(this, UiKit.chartCard(this, insights, I18n.t("Orders by status")),
                    Insights.countBy(orderList, o -> o.status, CompanyLabels.ORDER_STATUSES, CompanyLabels.ORDER_STATUS_NAMES), I18n.t("orders"));
            Cards.bars(this, UiKit.chartCard(this, insights, I18n.t("Top products (units sold)")), top(units, 5), 0xFF2E7D32, null);
            Charts.donut(this, UiKit.chartCard(this, insights, I18n.t("Service requests")),
                    Insights.countBy(serviceList, r -> r.status, CompanyLabels.HIRE_STATUS_KEYS, CompanyLabels.HIRE_STATUS_NAMES), I18n.t("requests"));
        }

        if (c.canBuy) {
            UiKit.section(this, insights, I18n.t("Buying"));
            int open = 0, waiting = 0, toConfirm = 0, toReceive = 0;
            for (BulkRequest r : requestList) { if ("OPEN".equals(r.status)) open++; waiting += (int) r.openQuotations; }
            for (BulkOrder o : poList) { if ("ISSUED".equals(o.status)) toConfirm++; if ("CONFIRMED".equals(o.status)) toReceive++; }
            List<View> t = new ArrayList<>();
            t.add(UiKit.statCard(this, "🌽", I18n.t("Open requests"), String.valueOf(open), I18n.t("taking quotations"), 0xFF2E7D32, v -> open(CompanyRequestsActivity.class)));
            t.add(UiKit.statCard(this, "📨", I18n.t("Quotations waiting"), String.valueOf(waiting), I18n.t("for your decision"), waiting > 0 ? 0xFFF9A825 : 0xFF2E7D32, v -> open(CompanyRequestsActivity.class)));
            t.add(UiKit.statCard(this, "📑", I18n.t("Orders to confirm"), String.valueOf(toConfirm), I18n.t("waiting for the farmer"), 0xFF1E88E5, v -> open(CompanyBulkOrdersActivity.class)));
            t.add(UiKit.statCard(this, "🚛", I18n.t("Produce to receive"), String.valueOf(toReceive), I18n.t("confirmed, not delivered"), 0xFF2E7D32, v -> open(CompanyBulkOrdersActivity.class)));
            UiKit.grid(this, insights, t);

            LinkedHashMap<String, Double> spend = Insights.months(6);
            for (BulkOrder o : poList) if (!"DECLINED".equals(o.status) && !"CANCELLED".equals(o.status)) Insights.add(spend, o.createdAt, o.total);
            LinearLayout a = UiKit.chartCard(this, insights, I18n.t("Purchases per month (KSh)"));
            a.addView(Charts.columns(this, spend, 0xFF2E7D32));
            Charts.donut(this, UiKit.chartCard(this, insights, I18n.t("Produce requests")),
                    Insights.countBy(requestList, r -> r.status, CompanyLabels.REQ_STATUS_KEYS, CompanyLabels.REQ_STATUS_NAMES), I18n.t("requests"));
            Charts.donut(this, UiKit.chartCard(this, insights, I18n.t("Purchase orders")),
                    Insights.countBy(poList, o -> o.status, CompanyLabels.PO_STATUS_KEYS, CompanyLabels.PO_STATUS_NAMES), I18n.t("orders"));
        }
    }

    private static Map<String, Long> top(Map<String, Long> m, int n) {
        List<Map.Entry<String, Long>> es = new ArrayList<>(m.entrySet());
        java.util.Collections.sort(es, (x, y) -> Long.compare(y.getValue(), x.getValue()));
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(n, es.size()); i++) out.put(es.get(i).getKey(), es.get(i).getValue());
        return out;
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

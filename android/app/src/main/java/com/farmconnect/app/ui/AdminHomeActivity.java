package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.AdminStats;
import com.farmconnect.app.data.Models.Analytics;
import com.farmconnect.app.data.Models.Indicators;
import com.farmconnect.app.data.Models.MarketOverview;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityAdminHomeBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AdminViewModel;
import com.farmconnect.app.vm.CommunityViewModel;
import com.farmconnect.app.vm.MarketViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Administrator console: platform numbers, things that need attention, and shortcuts to every admin tool. */
public class AdminHomeActivity extends AppCompatActivity {
    private Hub hub;
    private ActivityAdminHomeBinding b;
    private AdminViewModel vm;
    private CommunityViewModel cvm;
    private MarketViewModel mvm;
    private LinearLayout secMarket, secPeople, secProgramme, secCompanies, secCharts;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAdminHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        hub = Hub.attach(this, b.hubContainer);
        vm = new ViewModelProvider(this).get(AdminViewModel.class);

        String name = Session.name();
        b.tvTitle.setText(I18n.t("Hello, ") + (name == null || name.trim().isEmpty() ? I18n.t("Admin") : name.trim().split(" ")[0]));
        b.btnLogout.setOnClickListener(v -> Ui.logout(this));

        cvm = new ViewModelProvider(this).get(CommunityViewModel.class);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);

        View.OnClickListener verify = v -> startActivity(new Intent(this, ReviewerHomeActivity.class));
        View.OnClickListener orders = v -> startActivity(new Intent(this, OrdersActivity.class).putExtra("mode", "ADMIN"));
        b.tvAttention.setOnClickListener(verify);
        b.tvAllOrders.setOnClickListener(orders);

        secMarket = section(I18n.t("Marketplace today"));
        secPeople = section(I18n.t("People"));
        secProgramme = section(I18n.t("Programme activity"));
        secCompanies = "ADMIN".equals(Session.role()) ? section(I18n.t("Companies and marketplace")) : null;
        secCharts = section(I18n.t("Charts"));
    }

    /** A titled block of the overview. Its first child is the title; the rest is replaced on every refresh. */
    private LinearLayout section(String title) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        UiKit.section(this, box, title);
        b.dashContainer.addView(box, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private static void reset(LinearLayout sec) { if (sec != null && sec.getChildCount() > 1) sec.removeViews(1, sec.getChildCount() - 1); }

    @Override protected void onResume() {
        super.onResume();
        if (hub != null) hub.refresh();
        Ui.watch(this, vm.stats(), b.progress, this::showStats);
        Ui.watch(this, cvm.analytics(), null, this::showAnalytics);
        if (secCompanies != null) Ui.watch(this, mvm.overview(), null, this::showMarket);
        Ui.watch(this, vm.orders(), null, this::showRecent);
    }

    private static int pct(long n, long total) { return total <= 0 ? 0 : (int) Math.round(n * 100.0 / total); }

    private static String n(long v) { return String.valueOf(v); }

    private void showStats(AdminStats st) {
        long waiting = st.pendingVerifications + st.underReview;
        b.tvAttention.setVisibility(waiting > 0 ? View.VISIBLE : View.GONE);
        b.tvAttention.setText("⏳ " + waiting + (waiting == 1 ? I18n.t(" farmer is") : I18n.t(" farmers are")) + I18n.t(" waiting for verification  →"));

        reset(secMarket);
        List<View> t = new ArrayList<>();
        t.add(UiKit.statCard(this, "💰", I18n.t("Revenue (delivered orders)"), Ui.kes(st.revenue), null, 0xFFF9A825, null));
        t.add(UiKit.statCard(this, "🧾", I18n.t("Orders"), n(st.orders), st.pendingOrders + I18n.t(" pending · ") + st.deliveredOrders + I18n.t(" delivered"), 0xFF1E88E5, null));
        t.add(UiKit.statCard(this, "📦", I18n.t("Products"), n(st.activeProducts), I18n.t("of ") + st.products + I18n.t(" listed"), 0xFF2E7D32, null));
        t.add(UiKit.statCard(this, "🛡", I18n.t("Verification queue"), n(waiting), st.pendingVerifications + I18n.t(" waiting · ") + st.underReview + I18n.t(" under review"), 0xFF8E24AA,
                v -> startActivity(new Intent(this, ReviewerHomeActivity.class))));
        t.add(UiKit.statCard(this, "👥", I18n.t("Accounts"), n(st.users), st.officers + (st.officers == 1 ? I18n.t(" officer") : I18n.t(" officers")), 0xFF2E7D32,
                v -> startActivity(new Intent(this, AdminUsersActivity.class))));
        UiKit.grid(this, secMarket, t);
    }

    private void showAnalytics(Analytics a) {
        Indicators i = a.indicators;
        reset(secPeople);
        List<View> p = new ArrayList<>();
        p.add(UiKit.statCard(this, "🌱", I18n.t("Registered farmers"), n(i.registeredFarmers), i.verifiedFarmers + I18n.t(" verified"), 0xFF2E7D32, null));
        p.add(UiKit.statCard(this, "🛒", I18n.t("Registered buyers"), n(i.registeredBuyers), null, 0xFF8E24AA, null));
        p.add(UiKit.statCard(this, "📈", I18n.t("Active users (30 days)"), n(i.activeUsers30d), null, 0xFF1E88E5, null));
        p.add(UiKit.statCard(this, "🧑‍🤝‍🧑", I18n.t("Groups onboarded"), n(i.groupsOnboarded), i.groupMembers + (i.groupMembers == 1 ? I18n.t(" member") : I18n.t(" members")), 0xFF2E7D32, null));
        if (!a.scoped) p.add(UiKit.statCard(this, "🧑‍💼", I18n.t("Officers active (30 days)"), i.officersActive30d + I18n.t(" of ") + i.officersTotal, null, 0xFFF9A825, null));
        UiKit.grid(this, secPeople, p);

        reset(secProgramme);
        List<View> g = new ArrayList<>();
        g.add(UiKit.statCard(this, "🏪", I18n.t("Live market listings"), n(i.marketListings), null, 0xFF2E7D32, null));
        g.add(UiKit.statCard(this, "🤝", I18n.t("Farmer–buyer connections"), n(i.farmerBuyerConnections), null, 0xFF1E88E5, null));
        if (!a.scoped) {
            g.add(UiKit.statCard(this, "📚", I18n.t("Information interactions"), n(i.informationInteractions), null, 0xFF8E24AA, null));
            g.add(UiKit.statCard(this, "🎯", I18n.t("Opportunities accessed"), n(i.opportunitiesAccessed), null, 0xFFF9A825, null));
            g.add(UiKit.statCard(this, "📣", I18n.t("Programmes communicated"), n(i.programmesCommunicated), null, 0xFF2E7D32, null));
        }
        g.add(UiKit.statCard(this, "📒", I18n.t("Farmers using digital records"), n(i.farmersUsingRecords), i.recordsTotal + I18n.t(" records"), 0xFF1E88E5, null));
        if (!a.scoped)
            g.add(UiKit.statCard(this, "⭐", I18n.t("Farmer satisfaction"), i.satisfactionResponses == 0 ? "–" : String.format(Locale.US, "%.1f / 5", i.satisfactionAverage),
                    i.satisfactionResponses + (i.satisfactionResponses == 1 ? I18n.t(" vote") : I18n.t(" votes")), 0xFFF9A825, null));
        UiKit.grid(this, secProgramme, g);

        reset(secCharts);
        if (a.scoped && a.scopeLabel != null) secCharts.addView(Cards.text(this, I18n.t("Your area: ") + a.scopeLabel, 13, 0xFF5E7061, false));
        if (!a.scoped && a.farmersByCounty != null && a.farmersByCounty.size() > 1)
            Cards.bars(this, UiKit.chartCard(this, secCharts, I18n.t("Farmers by county")), a.farmersByCounty, 0xFF2E7D32, null);
        Cards.bars(this, UiKit.chartCard(this, secCharts, I18n.t("Farmers by sub-county")), a.farmersBySubCounty, 0xFF2E7D32, null);
        Charts.donut(this, UiKit.chartCard(this, secCharts, I18n.t("Live products by category")), a.productsByCategory, I18n.t("products"));
        LinearLayout orders = UiKit.chartCard(this, secCharts, I18n.t("Orders per month"));
        orders.addView(Charts.columns(this, a.ordersByMonth, 0xFF1E88E5));
        LinearLayout sales = UiKit.chartCard(this, secCharts, I18n.t("Sales per month (KES)"));
        sales.addView(Charts.columns(this, a.salesByMonth, 0xFF2E7D32));
        Cards.bars(this, UiKit.chartCard(this, secCharts, I18n.t("Recorded harvest by crop (kg)")), a.harvestKgByCrop, 0xFF8D6E63, "kg");
        Charts.donut(this, UiKit.chartCard(this, secCharts, I18n.t("Groups by type")), a.groupsByType, I18n.t("groups"));
    }

    private static long cnt(Map<String, Long> m, String k) { Long v = m == null ? null : m.get(k); return v == null ? 0 : v; }

    private void showMarket(MarketOverview o) {
        if (secCompanies == null) return;
        reset(secCompanies);
        View.OnClickListener open = v -> startActivity(new Intent(this, AdminMarketplaceActivity.class));
        View.OnClickListener companies = v -> startActivity(new Intent(this, AdminCompaniesActivity.class));
        long review = cnt(o.companies, "PENDING_VERIFICATION") + cnt(o.companies, "UNDER_REVIEW");
        List<View> t = new ArrayList<>();
        t.add(UiKit.statCard(this, "🏢", I18n.t("Verified companies"), n(cnt(o.companies, "VERIFIED")), review + I18n.t(" awaiting review"), review > 0 ? 0xFFF9A825 : 0xFF2E7D32, companies));
        t.add(UiKit.statCard(this, "🧺", I18n.t("Input sales delivered"), Ui.kes(o.supplyDeliveredGross), cnt(o.supplyOrders, "DELIVERED") + I18n.t(" orders"), 0xFF2E7D32, open));
        t.add(UiKit.statCard(this, "🚚", I18n.t("Open input orders"), n(cnt(o.supplyOrders, "PLACED") + cnt(o.supplyOrders, "CONFIRMED") + cnt(o.supplyOrders, "DISPATCHED")), I18n.t("new, confirmed or dispatched"), 0xFF1E88E5, open));
        t.add(UiKit.statCard(this, "🌽", I18n.t("Open produce requests"), n(cnt(o.produceRequests, "OPEN")), cnt(o.quotations, "SUBMITTED") + I18n.t(" quotations waiting"), 0xFF8E24AA, open));
        t.add(UiKit.statCard(this, "🛠", I18n.t("Service jobs"), n(cnt(o.serviceRequests, "REQUESTED") + cnt(o.serviceRequests, "ACCEPTED")), I18n.t("new or accepted"), 0xFFF9A825, open));
        UiKit.grid(this, secCompanies, t);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void showRecent(List<Order> list) {
        b.recentContainer.removeAllViews();
        b.tvNoOrders.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Order o = list.get(i);
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(16), dp(5), dp(16), dp(5));
            card.setLayoutParams(lp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(2));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setClickable(true);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            String cover = o.items.isEmpty() ? null : o.items.get(0).imageUrl;
            row.addView(Thumbs.make(this, cover, 44, 12));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView t1 = new TextView(this);
            t1.setText(I18n.t("Order #") + o.id + " · " + (o.buyerName == null ? I18n.t("Customer") : o.buyerName));
            t1.setTextColor(0xFF1C2B1E);
            t1.setTextSize(14);
            t1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            TextView t2 = new TextView(this);
            t2.setText(Ui.dateTime(o.createdAt));
            t2.setTextColor(0xFF5E7061);
            t2.setTextSize(12);
            col.addView(t1);
            col.addView(t2);
            row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);
            TextView total = new TextView(this);
            total.setText(Ui.kes(o.total));
            total.setTextColor(0xFF2E7D32);
            total.setTextSize(14);
            total.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            TextView st = new TextView(this);
            st.setTextColor(0xFFFFFFFF);
            st.setTextSize(10);
            st.setPadding(dp(8), dp(2), dp(8), dp(2));
            Ui.styleStatus(st, o.status);
            right.addView(total);
            right.addView(st);
            row.addView(right);

            card.addView(row);
            card.setOnClickListener(v -> startActivity(new Intent(this, OrderDetailActivity.class)
                    .putExtra("order", new Gson().toJson(o)).putExtra("mode", "ADMIN")));
            b.recentContainer.addView(card);
        }
    }
}

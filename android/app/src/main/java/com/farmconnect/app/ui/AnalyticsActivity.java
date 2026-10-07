package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.ApiClient;
import com.farmconnect.app.data.Models.Analytics;
import com.farmconnect.app.data.Models.Indicators;
import com.farmconnect.app.databinding.ActivitySimplePageBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;
import com.google.android.material.card.MaterialCardView;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** County analytics + programme monitoring indicators for admins and officers. */
public class AnalyticsActivity extends AppCompatActivity {
    private ActivitySimplePageBinding b;
    private CommunityViewModel vm;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySimplePageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        b.tvTitle.setText("County analytics");
        b.tvSubtitle.setText("Indicators and trends");
        b.btnBack.setOnClickListener(v -> finish());
        b.btnHeaderAction.setVisibility(View.VISIBLE);
        b.btnHeaderAction.setText("Share report");
        b.btnHeaderAction.setOnClickListener(v -> shareCsv());
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.watch(this, vm.analytics(), b.progress, this::render);
    }

    /** Downloads the CSV report from the server and hands it to any app (email, WhatsApp, Drive...). */
    private void shareCsv() {
        Ui.toast(this, "Preparing the report...");
        new Thread(() -> {
            try {
                retrofit2.Response<okhttp3.ResponseBody> r = ApiClient.get().analyticsCsv().execute();
                if (!r.isSuccessful() || r.body() == null) throw new RuntimeException("Server returned " + r.code());
                String csv = r.body().string();
                runOnUiThread(() -> startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
                        .setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "FarmConnect county report")
                        .putExtra(Intent.EXTRA_TEXT, csv), "Share the report")));
            } catch (Exception e) {
                runOnUiThread(() -> Ui.toast(this, "Could not get the report: " + e.getMessage()));
            }
        }).start();
    }

    private void tiles(LinearLayout parent, String[][] items) {
        for (int i = 0; i < items.length; i += 2) {
            LinearLayout row = Cards.statRow(this, parent);
            row.addView(Cards.stat(this, items[i][0], items[i][1]));
            if (i + 1 < items.length) row.addView(Cards.stat(this, items[i + 1][0], items[i + 1][1]));
            else row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        }
    }

    private LinearLayout section(String title) {
        MaterialCardView card = Cards.card(this, b.content, 12);
        LinearLayout in = Cards.inner(card);
        in.addView(Cards.title(this, title));
        return in;
    }

    private static String n(long v) { return String.valueOf(v); }

    private void render(Analytics a) {
        b.content.removeAllViews();
        Indicators i = a.indicators;
        TextView h = Cards.text(this, "Programme indicators", 18, 0xFF1C2B1E, true);
        h.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        b.content.addView(h);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        b.content.addView(grid);
        tiles(grid, new String[][]{
                {n(i.registeredFarmers), "Registered farmers"}, {n(i.verifiedFarmers), "Verified farmers"},
                {n(i.activeUsers30d), "Active users (30 days)"}, {n(i.registeredBuyers), "Registered buyers"},
                {n(i.groupsOnboarded), "Groups onboarded"}, {n(i.groupMembers), "Group members"},
                {n(i.informationInteractions), "Information interactions"}, {n(i.opportunitiesAccessed), "Opportunities accessed"},
                {n(i.marketListings), "Market listings"}, {n(i.farmerBuyerConnections), "Farmer–buyer connections"},
                {i.officersActive30d + "/" + i.officersTotal, "Officers active (30 days)"}, {n(i.programmesCommunicated), "Programmes communicated"},
                {n(i.farmersUsingRecords), "Farmers using digital records"}, {n(i.recordsTotal), "Farm records created"},
                {i.satisfactionResponses == 0 ? "–" : String.format(Locale.US, "%.1f / 5", i.satisfactionAverage), "Farmer satisfaction (" + i.satisfactionResponses + ")"},
                {n(i.messagesSent), "Messages exchanged"}});

        Map<String, Long> farmers = new LinkedHashMap<>(a.farmersBySubCounty);
        Cards.bars(this, section("Farmers by sub-county"), farmers, 0xFF2E7D32, null);
        Cards.bars(this, section("Verified farmers by sub-county"), a.verifiedBySubCounty, 0xFF1B5E20, null);
        Cards.bars(this, section("Live products by category"), a.productsByCategory, 0xFFF9A825, null);
        Cards.bars(this, section("Orders per month"), a.ordersByMonth, 0xFF1565C0, null);
        Cards.bars(this, section("Sales per month (KES)"), a.salesByMonth, 0xFF2E7D32, "KES");
        Cards.bars(this, section("Recorded harvest by crop (kg)"), a.harvestKgByCrop, 0xFF8D6E63, "kg");
        Cards.bars(this, section("Groups by type"), a.groupsByType, 0xFF6A1B9A, null);
        Cards.bars(this, section("Largest groups (members)"), a.membersByGroup, 0xFF00897B, null);
        Cards.bars(this, section("Information published by type"), a.postsByType, 0xFFC62828, null);
        b.content.addView(Cards.spacer(this, 20));
    }
}

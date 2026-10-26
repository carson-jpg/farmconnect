package com.farmconnect.app.ui;

import android.widget.LinearLayout;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Models.ReviewSummary;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityReviewListBinding;
import com.farmconnect.app.util.I18n;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.ReviewViewModel;
import com.google.android.material.chip.Chip;
import java.util.List;

/** Home screen for ADMIN and County Agricultural Officer accounts: stats, quick tools and the verification queue. */
public class ReviewerHomeActivity extends AppCompatActivity implements ReviewAdapter.Listener {
    private Hub hub;
    private ActivityReviewListBinding b;
    private ReviewViewModel vm;
    private ReviewAdapter adapter;
    private String status = "PENDING";
    private final java.util.Map<String, List<ReviewSummary>> queues = new java.util.LinkedHashMap<>();

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityReviewListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        hub = Hub.attach(this, b.hubContainer);
        vm = new ViewModelProvider(this).get(ReviewViewModel.class);

        String name = Session.name();
        b.tvOfficerName.setText(name == null || name.trim().isEmpty() ? I18n.t("Officer") : name.trim());
        b.tvRoleBadge.setText(Labels.roleName(Session.role()));

        b.btnLogout.setOnClickListener(v -> Ui.logout(this));
        adapter = new ReviewAdapter(this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);

        b.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            status = String.valueOf(c.getTag());
            load();
        });

        // The stat tiles double as filters.
        b.cardPending.setOnClickListener(v -> b.chipPending.setChecked(true));
        b.cardReview.setOnClickListener(v -> b.chipReview.setChecked(true));
        b.cardVerified.setOnClickListener(v -> b.chipVerified.setChecked(true));
        b.cardRejected.setOnClickListener(v -> b.chipRejected.setChecked(true));
    }

    @Override protected void onResume() {
        super.onResume();
        if (hub != null) hub.refresh();
        load();
        loadCounts();
    }

    private void load() {
        Ui.watch(this, vm.list(status), b.progress, list -> {
            adapter.set(list);
            boolean empty = list == null || list.isEmpty();
            b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            b.tvListCount.setVisibility(empty ? View.GONE : View.VISIBLE);
            b.tvListCount.setText(String.valueOf(list == null ? 0 : list.size()));
        });
    }

    /** Reads the size of each queue once, for the stat tiles. */
    private void loadCounts() {
        count("PENDING", b.tvCountPending, true);
        count("UNDER_REVIEW", b.tvCountReview, false);
        count("VERIFIED", b.tvCountVerified, false);
        count("REJECTED", b.tvCountRejected, false);
    }

    private void count(String st, TextView target, boolean isPending) {
        LiveData<Resource<List<ReviewSummary>>> ld = vm.list(st);
        ld.observe(this, new Observer<Resource<List<ReviewSummary>>>() {
            @Override public void onChanged(Resource<List<ReviewSummary>> r) {
                if (r.status == Resource.Status.LOADING) return;
                ld.removeObserver(this);
                if (r.status != Resource.Status.SUCCESS || r.data == null) return;
                target.setText(String.valueOf(r.data.size()));
                queues.put(st, r.data);
                showInsights();
                if (isPending) showAttention(r.data.size());
            }
        });
    }

    private void showAttention(int pending) {
        b.tvAttention.setText(pending > 0
                ? pending + " " + getString(R.string.off_stat_total)
                : getString(R.string.off_all_clear));
    }

    @Override public void onOpen(ReviewSummary r) {
        startActivity(new Intent(this, ReviewDetailActivity.class).putExtra("id", r.id));
    }

    @Override public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.menu_reviewer, m);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == R.id.action_logout) Ui.logout(this);
        return true;
    }

    /** Where the applications are in the process, when they arrive and where they come from. */
    private void showInsights() {
        LinearLayout box = b.insightsContainer;
        box.removeAllViews();
        UiKit.section(this, box, I18n.t("Insights"));
        java.util.LinkedHashMap<String, Long> byStatus = new java.util.LinkedHashMap<>();
        byStatus.put(I18n.t("Pending"), (long) size("PENDING"));
        byStatus.put(I18n.t("Under review"), (long) size("UNDER_REVIEW"));
        byStatus.put(I18n.t("Verified"), (long) size("VERIFIED"));
        byStatus.put(I18n.t("Rejected"), (long) size("REJECTED"));
        Charts.donut(this, UiKit.chartCard(this, box, I18n.t("Applications by status")), byStatus, I18n.t("applications"));
        java.util.LinkedHashMap<String, Double> months = Insights.months(6);
        List<ReviewSummary> all = new java.util.ArrayList<>();
        for (List<ReviewSummary> l : queues.values()) all.addAll(l);
        for (ReviewSummary r : all) Insights.add(months, r.submittedAt, 1);
        LinearLayout c = UiKit.chartCard(this, box, I18n.t("Applications per month"));
        c.addView(Charts.columns(this, months, 0xFF1E88E5));
        Cards.bars(this, UiKit.chartCard(this, box, I18n.t("Applications by sub-county")), Insights.topCounts(all, r -> r.subCounty, 8), 0xFF2E7D32, null);
    }

    private int size(String st) { List<ReviewSummary> l = queues.get(st); return l == null ? 0 : l.size(); }
}

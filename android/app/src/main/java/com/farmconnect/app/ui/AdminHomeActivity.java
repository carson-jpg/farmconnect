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
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityAdminHomeBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AdminViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import java.util.List;

/** Administrator console: platform numbers, things that need attention, and shortcuts to every admin tool. */
public class AdminHomeActivity extends AppCompatActivity {
    private Hub hub;
    private ActivityAdminHomeBinding b;
    private AdminViewModel vm;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAdminHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        hub = Hub.attach(this, b.hubContainer);
        vm = new ViewModelProvider(this).get(AdminViewModel.class);

        String name = Session.name();
        b.tvTitle.setText(I18n.t("Hello, ") + (name == null || name.trim().isEmpty() ? I18n.t("Admin") : name.trim().split(" ")[0]));
        b.btnLogout.setOnClickListener(v -> Ui.logout(this));

        View.OnClickListener verify = v -> startActivity(new Intent(this, ReviewerHomeActivity.class));
        View.OnClickListener orders = v -> startActivity(new Intent(this, OrdersActivity.class).putExtra("mode", "ADMIN"));
        b.actVerify.setOnClickListener(verify);
        b.tvAttention.setOnClickListener(verify);
        b.actUsers.setOnClickListener(v -> startActivity(new Intent(this, AdminUsersActivity.class)));
        b.actOrders.setOnClickListener(orders);
        b.tvAllOrders.setOnClickListener(orders);
        b.actProducts.setOnClickListener(v -> startActivity(new Intent(this, AdminProductsActivity.class)));
    }

    @Override protected void onResume() {
        super.onResume();
        if (hub != null) hub.refresh();
        Ui.watch(this, vm.stats(), b.progress, this::showStats);
        Ui.watch(this, vm.orders(), null, this::showRecent);
    }

    private static int pct(long n, long total) { return total <= 0 ? 0 : (int) Math.round(n * 100.0 / total); }

    private void showStats(AdminStats st) {
        b.tvRevenue.setText(Ui.kes(st.revenue));
        b.tvRevenueSub.setText(st.orders + I18n.t(" orders · ") + st.deliveredOrders + I18n.t(" delivered"));
        b.tvUsers.setText(String.valueOf(st.users));
        b.tvFarmers.setText(st.verifiedFarmers + " / " + st.farmers);
        b.tvOrders.setText(String.valueOf(st.orders));
        b.tvProducts.setText(st.activeProducts + " / " + st.products);

        long waiting = st.pendingVerifications + st.underReview;
        b.tvAttention.setVisibility(waiting > 0 ? View.VISIBLE : View.GONE);
        b.tvAttention.setText("⏳ " + waiting + (waiting == 1 ? I18n.t(" farmer is") : I18n.t(" farmers are")) + I18n.t(" waiting for verification  →"));

        b.tvPendingN.setText(String.valueOf(st.pendingOrders));
        b.tvDeliveredN.setText(String.valueOf(st.deliveredOrders));
        b.tvCancelledN.setText(String.valueOf(st.cancelledOrders));
        b.pbPending.setProgress(pct(st.pendingOrders, st.orders));
        b.pbDelivered.setProgress(pct(st.deliveredOrders, st.orders));
        b.pbCancelled.setProgress(pct(st.cancelledOrders, st.orders));
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

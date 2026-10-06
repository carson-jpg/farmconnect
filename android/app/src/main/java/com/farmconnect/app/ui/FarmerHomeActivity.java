package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Models.OrderItem;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.data.Models.VerificationResponse;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityFarmerDashboardBinding;
import com.farmconnect.app.databinding.ItemOrderMiniBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;
import com.farmconnect.app.vm.VerificationViewModel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Farmer dashboard: verification status, stats, quick actions, recent orders. */
public class FarmerHomeActivity extends AppCompatActivity {
    private ActivityFarmerDashboardBinding b;
    private FarmerViewModel vm;
    private VerificationViewModel verifyVm;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityFarmerDashboardBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);
        verifyVm = new ViewModelProvider(this).get(VerificationViewModel.class);

        String name = Session.name();
        b.tvGreeting.setText(name == null || name.trim().isEmpty() ? "Farmer" : name.trim().split(" ")[0]);

        b.btnLogout.setOnClickListener(v -> Ui.logout(this));
        b.cardVerify.setOnClickListener(v -> startActivity(new Intent(this, VerificationActivity.class)));
        b.actAddProduct.setOnClickListener(v ->
                startActivity(new Intent(this, FarmerProductsActivity.class).putExtra("add", true)));
        b.actFarms.setOnClickListener(v -> startActivity(new Intent(this, FarmsActivity.class)));
        b.actProducts.setOnClickListener(v -> startActivity(new Intent(this, FarmerProductsActivity.class)));
        View.OnClickListener openOrders = v ->
                startActivity(new Intent(this, OrdersActivity.class).putExtra("farmer", true));
        b.actOrders.setOnClickListener(openOrders);
        b.tvSeeAll.setOnClickListener(openOrders);
        b.cardLowStock.setOnClickListener(v -> startActivity(new Intent(this, FarmerProductsActivity.class)));
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.watch(this, verifyVm.mine(), null, this::showVerification);
        load();
    }

    private void showVerification(VerificationResponse v) {
        String title, msg;
        int bg;
        switch (v.status == null ? "DRAFT" : v.status) {
            case "PENDING":
                bg = 0xFFE3F2FD; title = "Pending verification";
                msg = "We received your details. A reviewer will pick them up soon."; break;
            case "UNDER_REVIEW":
                bg = 0xFFEDE7F6; title = "Under review";
                msg = "A reviewer is checking your details now."; break;
            case "VERIFIED":
                bg = 0xFFE3F1E0; title = "✓ Verified Farmer";
                msg = "Your identity and farm are verified. Buyers see your badge."; break;
            case "REJECTED":
                bg = 0xFFFDECEA; title = "Verification rejected";
                msg = (v.rejectionReason == null ? "" : v.rejectionReason + " ") + "Tap to fix and submit again."; break;
            default:
                bg = 0xFFFFF4D6; title = "Get verified";
                msg = "Verify your identity and farm to earn the ✓ Verified Farmer badge.";
        }
        b.cardVerify.setCardBackgroundColor(bg);
        b.tvVerifyTitle.setText(title);
        b.tvVerifyMsg.setText(msg);
        b.tvBadge.setVisibility("VERIFIED".equals(v.status) ? View.VISIBLE : View.GONE);
    }

    private void load() {
        Ui.watch(this, vm.myFarms(), b.progress, farms -> {
            b.tvFarms.setText(String.valueOf(farms.size()));
            Set<Long> farmIds = new HashSet<>();
            for (Farm f : farms) farmIds.add(f.id);

            Ui.watch(this, vm.products(), b.progress, all -> {
                Set<Long> myProductIds = new HashSet<>();
                int count = 0, low = 0;
                for (Product p : all) {
                    if (!farmIds.contains(p.farmId)) continue;
                    myProductIds.add(p.id);
                    count++;
                    if (p.quantity <= 5) low++;
                }
                b.tvProducts.setText(String.valueOf(count));
                b.cardLowStock.setVisibility(low > 0 ? View.VISIBLE : View.GONE);
                b.tvLowStock.setText("⚠ " + low + (low == 1 ? " product is" : " products are")
                        + " running low on stock (5 or fewer). Tap to restock.");

                Ui.watch(this, vm.orders(), b.progress, orders -> showOrders(orders, myProductIds));
            });
        });
    }

    private void showOrders(List<Order> orders, Set<Long> myProductIds) {
        int pending = 0;
        double sales = 0;
        for (Order o : orders) {
            if ("PENDING".equals(o.status)) pending++;
            if ("CANCELLED".equals(o.status)) continue;
            for (OrderItem i : o.items) if (myProductIds.contains(i.productId)) sales += i.price * i.quantity;
        }
        b.tvPending.setText(String.valueOf(pending));
        b.tvSales.setText(Ui.kes(sales));

        b.ordersContainer.removeAllViews();
        b.tvNoOrders.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < Math.min(3, orders.size()); i++) {
            Order o = orders.get(i);
            ItemOrderMiniBinding m = ItemOrderMiniBinding.inflate(getLayoutInflater(), b.ordersContainer, false);
            int n = o.items == null ? 0 : o.items.size();
            m.tvTitle.setText("Order #" + o.id + " · " + n + (n == 1 ? " item" : " items"));
            String who = o.buyerName == null || o.buyerName.isEmpty() ? "Customer" : o.buyerName;
            m.tvSub.setText(who + " · " + Ui.kes(o.total));
            Ui.styleStatus(m.tvStatus, o.status);
            m.getRoot().setOnClickListener(v ->
                    startActivity(new Intent(this, OrdersActivity.class).putExtra("farmer", true)));
            b.ordersContainer.addView(m.getRoot());
        }
    }
}
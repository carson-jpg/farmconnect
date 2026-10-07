package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.farmconnect.app.R;
import com.farmconnect.app.data.ApiClient;
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
import com.google.android.material.card.MaterialCardView;
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
        b.actAddProduct.setOnClickListener(v -> startActivity(new Intent(this, ProductEditActivity.class)));
        b.tvManage.setOnClickListener(v -> startActivity(new Intent(this, FarmerProductsActivity.class)));
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
                msg = "We received your details. Your products stay hidden from buyers until you are approved."; break;
            case "UNDER_REVIEW":
                bg = 0xFFEDE7F6; title = "Under review";
                msg = "A reviewer is checking your details now. Your products go public once approved."; break;
            case "VERIFIED":
                bg = 0xFFE3F1E0; title = "✓ Verified Farmer";
                msg = "You are live! Buyers can see your farm and products, with your Verified badge."; break;
            case "REJECTED":
                bg = 0xFFFDECEA; title = "Verification rejected";
                msg = (v.rejectionReason == null ? "" : v.rejectionReason + " ") + "Tap to fix and submit again. Products stay hidden until approved."; break;
            default:
                bg = 0xFFFFF4D6; title = "Get verified";
                msg = "Verify your identity and farm so buyers can see your products. Unverified farmers are not listed.";
        }
        b.cardVerify.setCardBackgroundColor(bg);
        b.tvVerifyTitle.setText(title);
        b.tvVerifyMsg.setText(msg);
        b.tvBadge.setVisibility("VERIFIED".equals(v.status) ? View.VISIBLE : View.GONE);
    }

    private void load() {
        Ui.watch(this, vm.myFarms(), b.progress, farms -> {
            b.tvFarms.setText(String.valueOf(farms.size()));
            Ui.watch(this, vm.myProducts(), b.progress, mine -> {
                Set<Long> myProductIds = new HashSet<>();
                int low = 0;
                for (Product p : mine) {
                    myProductIds.add(p.id);
                    if (p.quantity <= 5) low++;
                }
                b.tvProducts.setText(String.valueOf(mine.size()));
                b.cardLowStock.setVisibility(low > 0 ? View.VISIBLE : View.GONE);
                b.tvLowStock.setText("⚠ " + low + (low == 1 ? " product is" : " products are")
                        + " running low on stock (5 or fewer). Tap to restock.");
                showProducts(mine);

                Ui.watch(this, vm.orders(), b.progress, orders -> showOrders(orders, myProductIds));
            });
        });
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** Horizontal strip of my latest products with their cover photo. */
    private void showProducts(List<Product> mine) {
        b.productsContainer.removeAllViews();
        b.tvNoProducts.setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < Math.min(8, mine.size()); i++) {
            Product p = mine.get(mine.size() - 1 - i);   // newest first
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(150), LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(6), dp(4), dp(6), dp(8));
            card.setLayoutParams(lp);
            card.setRadius(dp(16));
            card.setCardElevation(dp(3));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setClickable(true);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            FrameLayout photo = new FrameLayout(this);
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.addView(iv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(105)));
            String cover = p.imageUrls != null && !p.imageUrls.isEmpty() ? p.imageUrls.get(0) : p.imageUrl;
            if (cover != null && !cover.isEmpty()) {
                Glide.with(this).load(ApiClient.absolute(cover)).centerCrop()
                        .placeholder(R.drawable.ph_product).error(R.drawable.ph_product).into(iv);
            } else {
                iv.setImageResource(R.drawable.ph_product);
            }
            if (!p.farmerVerified) {
                TextView hidden = new TextView(this);
                hidden.setText("Hidden");
                hidden.setTextSize(10);
                hidden.setTextColor(0xFF7A5600);
                hidden.setBackgroundResource(R.drawable.bg_chip);
                hidden.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF4D6));
                hidden.setPadding(dp(8), dp(2), dp(8), dp(2));
                FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                hp.gravity = Gravity.TOP | Gravity.START;
                hp.setMargins(dp(8), dp(8), 0, 0);
                photo.addView(hidden, hp);
            }
            col.addView(photo);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(10), dp(8), dp(10), dp(10));
            TextView name = new TextView(this);
            name.setText(p.name);
            name.setTextSize(14);
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            name.setTextColor(0xFF1C2B1E);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView price = new TextView(this);
            price.setText(Ui.kes(p.price));
            price.setTextSize(13);
            price.setTextColor(0xFF2E7D32);
            info.addView(name);
            info.addView(price);
            col.addView(info);
            card.addView(col);
            card.setOnClickListener(v -> startActivity(new Intent(this, ProductEditActivity.class).putExtra("id", p.id)));
            b.productsContainer.addView(card);
        }
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
package com.farmconnect.app.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityBuyerHomeBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

/** Marketplace home for buyers. The server only returns products of VERIFIED farmers. */
public class BuyerHomeActivity extends AppCompatActivity implements ProductAdapter.Listener {
    private Hub hub;
    private ActivityBuyerHomeBinding b;
    private BuyerViewModel vm;
    private ProductAdapter adapter;
    private List<Product> all = new ArrayList<>();
    private String category = "All";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityBuyerHomeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        hub = Hub.attach(this, b.hubContainer);
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);

        String name = Session.name();
        b.tvGreeting.setText(name == null || name.trim().isEmpty() ? "Friend" : name.trim().split(" ")[0]);

        adapter = new ProductAdapter(ProductAdapter.Mode.BUYER, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);

        b.btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        b.btnWishlist.setOnClickListener(v -> startActivity(new Intent(this, WishlistActivity.class)));
        b.btnOrders.setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)));
        b.btnLogout.setOnClickListener(v -> Ui.logout(this));
        b.etSearch.setOnEditorActionListener((v, a, e) -> { load(); return true; });

        buildChips();
    }

    @Override protected void onResume() {
        super.onResume();
        if (hub != null) hub.refresh();
        load();
    }

    private void buildChips() {
        List<String> names = new ArrayList<>();
        names.add("All");
        names.addAll(ProductEditActivity.CATEGORIES);
        ColorStateList bg = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}}, new int[]{0xFF2E7D32, 0xFFFFFFFF});
        ColorStateList fg = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}}, new int[]{0xFFFFFFFF, 0xFF1C2B1E});
        for (String n : names) {
            Chip c = new Chip(this);
            c.setId(View.generateViewId());
            c.setText(n);
            c.setTag(n);
            c.setCheckable(true);
            c.setCheckedIconVisible(false);
            c.setChipBackgroundColor(bg);
            c.setTextColor(fg);
            c.setChipStrokeColor(ColorStateList.valueOf(0xFFD5E3D0));
            c.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            c.setChecked(n.equals("All"));
            b.chipGroup.addView(c);
        }
        b.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            category = String.valueOf(c.getTag());
            applyFilter();
        });
    }

    private void load() {
        Ui.watch(this, vm.products(b.etSearch.getText().toString().trim()), b.progress, list -> {
            all = list == null ? new ArrayList<>() : list;
            applyFilter();
        });
    }

    private void applyFilter() {
        List<Product> shown = new ArrayList<>();
        for (Product p : all) {
            if ("All".equals(category) || category.equalsIgnoreCase(p.category)) shown.add(p);
        }
        adapter.set(shown);
        b.tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvCount.setText(shown.isEmpty() ? "Marketplace"
                : shown.size() + (shown.size() == 1 ? " product" : " products") + ("All".equals(category) ? "" : " · " + category));
    }

    @Override public void onPrimary(Product p) {
        Ui.watch(this, vm.addToCart(p.id, 1), null, c -> Ui.toast(this, "Added to cart"));
    }

    @Override public void onSecondary(Product p) {
        Ui.watch(this, vm.addWish(p.id), null, x -> Ui.toast(this, "Saved to wishlist"));
    }
}

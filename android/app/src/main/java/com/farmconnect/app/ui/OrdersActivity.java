package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.databinding.ActivityOrdersBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import com.farmconnect.app.vm.FarmerViewModel;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

/** Used by buyers (my orders) and farmers (intent extra "farmer" = true). */
public class OrdersActivity extends AppCompatActivity implements OrderAdapter.Listener {
    private ActivityOrdersBinding b;
    private BuyerViewModel buyerVm;
    private FarmerViewModel farmerVm;
    private OrderAdapter adapter;
    private boolean farmer;
    private List<Order> all = new ArrayList<>();
    private String filter = "ALL";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        farmer = getIntent().getBooleanExtra("farmer", false);
        setTitle(farmer ? "Customer orders" : "My orders");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        buyerVm = new ViewModelProvider(this).get(BuyerViewModel.class);
        farmerVm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adapter = new OrderAdapter(farmer, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);

        b.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            filter = String.valueOf(c.getTag());
            apply();
        });
        load();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        Ui.watch(this, farmer ? farmerVm.orders() : buyerVm.orders(), b.progress, list -> {
            all = list;
            apply();
        });
    }

    private void apply() {
        List<Order> shown = new ArrayList<>();
        for (Order o : all) if ("ALL".equals(filter) || filter.equals(o.status)) shown.add(o);
        adapter.set(shown);
        b.tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override public void onAction(Order o) {
        if (farmer) Ui.watch(this, farmerVm.updateStatus(o.id, OrderAdapter.next(o.status)), b.progress, x -> load());
        else Ui.watch(this, buyerVm.cancelOrder(o.id), b.progress, x -> load());
    }
}
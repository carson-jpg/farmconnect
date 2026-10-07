package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.databinding.ActivityOrdersBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AdminViewModel;
import com.farmconnect.app.vm.BuyerViewModel;
import com.farmconnect.app.vm.FarmerViewModel;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

/** Orders list for buyers ("My orders"), farmers (extra farmer=true / mode=FARMER) and admins (mode=ADMIN). */
public class OrdersActivity extends AppCompatActivity implements OrderAdapter.Listener {
    private ActivityOrdersBinding b;
    private BuyerViewModel buyerVm;
    private FarmerViewModel farmerVm;
    private AdminViewModel adminVm;
    private OrderAdapter adapter;
    private OrderAdapter.Mode mode = OrderAdapter.Mode.BUYER;
    private List<Order> all = new ArrayList<>();
    private String filter = "ALL";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        String m = getIntent().getStringExtra("mode");
        if (m != null) mode = OrderAdapter.Mode.valueOf(m);
        else if (getIntent().getBooleanExtra("farmer", false)) mode = OrderAdapter.Mode.FARMER;

        b = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        buyerVm = new ViewModelProvider(this).get(BuyerViewModel.class);
        farmerVm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adminVm = new ViewModelProvider(this).get(AdminViewModel.class);

        b.tvTitle.setText(mode == OrderAdapter.Mode.BUYER ? "My orders"
                : mode == OrderAdapter.Mode.FARMER ? "Customer orders" : "All orders");
        b.tvEmptyText.setText(mode == OrderAdapter.Mode.BUYER
                ? "You have not ordered anything yet.\nFresh produce is waiting in the market."
                : "No orders here yet");
        b.btnBack.setOnClickListener(v -> finish());

        adapter = new OrderAdapter(mode, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            filter = String.valueOf(c.getTag());
            apply();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        LiveData<Resource<List<Order>>> src = mode == OrderAdapter.Mode.ADMIN ? adminVm.orders()
                : mode == OrderAdapter.Mode.FARMER ? farmerVm.orders() : buyerVm.orders();
        Ui.watch(this, src, b.progress, list -> { all = list; apply(); });
    }

    private void apply() {
        List<Order> shown = new ArrayList<>();
        for (Order o : all) if ("ALL".equals(filter) || filter.equals(o.status)) shown.add(o);
        adapter.set(shown);
        b.tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvSubtitle.setText(all.size() + (all.size() == 1 ? " order" : " orders"));
    }

    @Override public void onAction(Order o) {
        switch (mode) {
            case ADMIN:
                Ui.watch(this, adminVm.setOrderStatus(o.id, OrderAdapter.next(o.status)), b.progress, x -> load());
                break;
            case FARMER:
                Ui.watch(this, farmerVm.updateStatus(o.id, OrderAdapter.next(o.status)), b.progress, x -> load());
                break;
            default:
                confirmCancel(o, () -> Ui.watch(this, buyerVm.cancelOrder(o.id), b.progress, x -> load()));
        }
    }

    @Override public void onSecondary(Order o) {
        confirmCancel(o, () -> Ui.watch(this, adminVm.setOrderStatus(o.id, "CANCELLED"), b.progress, x -> load()));
    }

    private void confirmCancel(Order o, Runnable run) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel order #" + o.id + "?")
                .setMessage("The reserved stock goes back to the seller. This cannot be undone.")
                .setPositiveButton("Cancel order", (d, w) -> run.run())
                .setNegativeButton("Keep order", null)
                .show();
    }
}

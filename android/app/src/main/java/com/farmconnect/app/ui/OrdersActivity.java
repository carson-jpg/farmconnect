package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import com.farmconnect.app.vm.FarmerViewModel;

/** Used by buyers (my orders) and farmers (intent extra "farmer" = true). */
public class OrdersActivity extends AppCompatActivity implements OrderAdapter.Listener {
    private ActivityListBinding b;
    private BuyerViewModel buyerVm;
    private FarmerViewModel farmerVm;
    private OrderAdapter adapter;
    private boolean farmer;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        farmer = getIntent().getBooleanExtra("farmer", false);
        setTitle(farmer ? "Customer orders" : "My orders");
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        buyerVm = new ViewModelProvider(this).get(BuyerViewModel.class);
        farmerVm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adapter = new OrderAdapter(farmer, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        load();
    }

    private void load() {
        Ui.watch(this, farmer ? farmerVm.orders() : buyerVm.orders(), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onAction(Order o) {
        if (farmer) Ui.watch(this, farmerVm.updateStatus(o.id, OrderAdapter.next(o.status)), null, x -> load());
        else Ui.watch(this, buyerVm.cancelOrder(o.id), null, x -> load());
    }
}

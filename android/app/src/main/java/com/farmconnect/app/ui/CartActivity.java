package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.CartItem;
import com.farmconnect.app.databinding.ActivityCartBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;

public class CartActivity extends AppCompatActivity implements CartAdapter.Listener {
    private ActivityCartBinding b;
    private BuyerViewModel vm;
    private CartAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("My cart");
        b = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);
        adapter = new CartAdapter(this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);

        b.btnOrder.setOnClickListener(v -> {
            String addr = b.etAddress.getText().toString().trim();
            if (addr.isEmpty()) { Ui.toast(this, "Enter delivery address"); return; }
            Ui.watch(this, vm.placeOrder(addr), b.progress, o -> {
                Ui.toast(this, "Order placed");
                startActivity(new Intent(this, OrdersActivity.class));
                finish();
            });
        });
        load();
    }

    private void load() {
        Ui.watch(this, vm.cart(), b.progress, items -> {
            adapter.set(items);
            double total = 0;
            for (CartItem c : items) total += c.product.price * c.quantity;
            b.tvTotal.setText("Total: " + Ui.kes(total));
            b.btnOrder.setEnabled(!items.isEmpty());
        });
    }

    @Override public void onPlus(CartItem i) {
        Ui.watch(this, vm.setQty(i.id, i.quantity + 1), null, x -> load());
    }

    @Override public void onMinus(CartItem i) {
        if (i.quantity > 1) Ui.watch(this, vm.setQty(i.id, i.quantity - 1), null, x -> load());
        else onRemove(i);
    }

    @Override public void onRemove(CartItem i) {
        Ui.watch(this, vm.removeCartItem(i.id), null, x -> load());
    }
}

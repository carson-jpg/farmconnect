package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.CartItem;
import com.farmconnect.app.databinding.ActivityCartBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.Listener {
    private ActivityCartBinding b;
    private BuyerViewModel vm;
    private CartAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);
        adapter = new CartAdapter(this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.tvTitle.setText("My cart");
        b.btnBack.setOnClickListener(v -> finish());
        b.btnShop.setOnClickListener(v -> finish());
        b.btnCheckout.setOnClickListener(v -> startActivity(new Intent(this, CheckoutActivity.class)));
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Ui.watch(this, vm.cart(), b.progress, this::render);
    }

    private void render(List<CartItem> items) {
        adapter.set(items);
        double total = 0;
        int count = 0;
        for (CartItem c : items) { total += c.product.price * c.quantity; count += c.quantity; }
        boolean empty = items.isEmpty();
        b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        b.rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.summaryBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        b.tvSubtitle.setText(empty ? "" : count + (count == 1 ? " item" : " items") + " ready for checkout");
        b.tvSubtotal.setText(Ui.kes(total));
        b.tvTotal.setText(Ui.kes(total));
    }

    @Override public void onPlus(CartItem i) {
        if (i.quantity >= i.product.quantity) { Ui.toast(this, "Only " + i.product.quantity + " in stock"); return; }
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

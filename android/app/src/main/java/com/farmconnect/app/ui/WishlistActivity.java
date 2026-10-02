package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;

public class WishlistActivity extends AppCompatActivity implements ProductAdapter.Listener {
    private ActivityListBinding b;
    private BuyerViewModel vm;
    private ProductAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("Wishlist");
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);
        adapter = new ProductAdapter(ProductAdapter.Mode.WISHLIST, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        load();
    }

    private void load() {
        Ui.watch(this, vm.wishlist(), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onPrimary(Product p) {
        Ui.watch(this, vm.addToCart(p.id, 1), null, c -> Ui.toast(this, "Added to cart"));
    }

    @Override public void onSecondary(Product p) {
        Ui.watch(this, vm.removeWish(p.id), null, x -> load());
    }
}

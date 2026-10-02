package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;

public class BuyerHomeActivity extends AppCompatActivity implements ProductAdapter.Listener {
    private ActivityListBinding b;
    private BuyerViewModel vm;
    private ProductAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("FarmConnect Market");
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);
        adapter = new ProductAdapter(ProductAdapter.Mode.BUYER, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.etSearch.setVisibility(View.VISIBLE);
        b.etSearch.setOnEditorActionListener((v, a, e) -> { load(); return true; });
        load();
    }

    private void load() {
        Ui.watch(this, vm.products(b.etSearch.getText().toString().trim()), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onPrimary(Product p) {
        Ui.watch(this, vm.addToCart(p.id, 1), null, c -> Ui.toast(this, "Added to cart"));
    }

    @Override public void onSecondary(Product p) {
        Ui.watch(this, vm.addWish(p.id), null, x -> Ui.toast(this, "Saved to wishlist"));
    }

    @Override public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.menu_buyer, m);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        int id = i.getItemId();
        if (id == R.id.action_cart) startActivity(new Intent(this, CartActivity.class));
        else if (id == R.id.action_wishlist) startActivity(new Intent(this, WishlistActivity.class));
        else if (id == R.id.action_orders) startActivity(new Intent(this, OrdersActivity.class));
        else if (id == R.id.action_logout) Ui.logout(this);
        return true;
    }
}

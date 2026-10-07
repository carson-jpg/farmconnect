package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;

/** The farmer's own products (visible to buyers only once the farmer is verified). */
public class FarmerProductsActivity extends AppCompatActivity implements ProductAdapter.Listener {
    private ActivityListBinding b;
    private FarmerViewModel vm;
    private ProductAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("My products");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adapter = new ProductAdapter(ProductAdapter.Mode.FARMER, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.tvEmpty.setText("No products yet. Tap + to add your first one, with photos and a full description.");
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> startActivity(new Intent(this, ProductEditActivity.class)));
        if (getIntent().getBooleanExtra("add", false)) {
            getIntent().removeExtra("add");
            startActivity(new Intent(this, ProductEditActivity.class));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        Ui.watch(this, vm.myProducts(), b.progress, mine -> {
            adapter.set(mine);
            b.tvEmpty.setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onPrimary(Product p) {
        startActivity(new Intent(this, ProductEditActivity.class).putExtra("id", p.id));
    }

    @Override public void onSecondary(Product p) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + p.name + "?")
                .setPositiveButton("Delete", (x, y) -> Ui.watch(this, vm.deleteProduct(p.id), b.progress, r -> {
                    Ui.toast(this, "Product deleted");
                    load();
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}

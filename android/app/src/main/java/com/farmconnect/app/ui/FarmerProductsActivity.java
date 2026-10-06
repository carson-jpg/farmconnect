package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.data.Models.ProductRequest;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.databinding.DialogProductBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FarmerProductsActivity extends AppCompatActivity implements ProductAdapter.Listener {
    private ActivityListBinding b;
    private FarmerViewModel vm;
    private ProductAdapter adapter;
    private List<Farm> farms = new ArrayList<>();
    private boolean openAdd;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("My products");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        openAdd = getIntent().getBooleanExtra("add", false);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adapter = new ProductAdapter(ProductAdapter.Mode.FARMER, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.tvEmpty.setText("No products yet. Tap + to add one.");
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> showDialog(null));
        load();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        Ui.watch(this, vm.myFarms(), b.progress, fs -> {
            farms = fs;
            Ui.watch(this, vm.products(), b.progress, all -> {
                Set<Long> ids = new HashSet<>();
                for (Farm f : farms) ids.add(f.id);
                List<Product> mine = new ArrayList<>();
                for (Product p : all) if (ids.contains(p.farmId)) mine.add(p);
                adapter.set(mine);
                b.tvEmpty.setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
                if (openAdd) { openAdd = false; showDialog(null); }
            });
        });
    }

    private static String num(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }

    private void showDialog(Product ex) {
        if (farms.isEmpty()) { Ui.toast(this, "Create a farm first (My farms)"); return; }
        DialogProductBinding d = DialogProductBinding.inflate(getLayoutInflater());
        d.spFarm.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, farms));
        if (ex != null) {
            d.etName.setText(ex.name);
            d.etCategory.setText(ex.category);
            d.etPrice.setText(num(ex.price));
            d.etQty.setText(String.valueOf(ex.quantity));
            d.etUnit.setText(ex.unit);
            for (int i = 0; i < farms.size(); i++) if (farms.get(i).id == ex.farmId) d.spFarm.setSelection(i);
        }
        new AlertDialog.Builder(this)
                .setTitle(ex == null ? "New product" : "Edit product")
                .setView(d.getRoot())
                .setPositiveButton("Save", (x, y) -> {
                    try {
                        String name = d.etName.getText().toString().trim();
                        if (name.isEmpty()) { Ui.toast(this, "Name required"); return; }
                        double price = Double.parseDouble(d.etPrice.getText().toString().trim());
                        int qty = Integer.parseInt(d.etQty.getText().toString().trim());
                        Farm farm = (Farm) d.spFarm.getSelectedItem();
                        ProductRequest r = new ProductRequest(name, d.etCategory.getText().toString().trim(),
                                price, qty, d.etUnit.getText().toString().trim(), farm.id);
                        if (ex == null) {
                            Ui.watch(this, vm.createProduct(r), b.progress, p -> load());
                        } else {
                            r.description = ex.description;
                            r.imageUrl = ex.imageUrl;
                            Ui.watch(this, vm.updateProduct(ex.id, r), b.progress, p -> load());
                        }
                    } catch (NumberFormatException e) {
                        Ui.toast(this, "Enter a valid price and quantity");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onPrimary(Product p) { showDialog(p); }

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
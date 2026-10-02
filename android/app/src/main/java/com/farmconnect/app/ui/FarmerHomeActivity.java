package com.farmconnect.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Models.FarmRequest;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.databinding.DialogFarmBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;

public class FarmerHomeActivity extends AppCompatActivity {
    private ActivityListBinding b;
    private FarmerViewModel vm;
    private FarmAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("My farms");
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adapter = new FarmAdapter();
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> showAddFarm());
        load();
    }

    private void load() {
        Ui.watch(this, vm.myFarms(), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showAddFarm() {
        DialogFarmBinding d = DialogFarmBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setTitle("New farm")
                .setView(d.getRoot())
                .setPositiveButton("Save", (x, y) -> {
                    String name = d.etName.getText().toString().trim();
                    if (name.isEmpty()) { Ui.toast(this, "Farm name required"); return; }
                    FarmRequest r = new FarmRequest(name, d.etLocation.getText().toString().trim(),
                            d.etDesc.getText().toString().trim());
                    Ui.watch(this, vm.createFarm(r), b.progress, f -> load());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.menu_farmer, m);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        int id = i.getItemId();
        if (id == R.id.action_products) startActivity(new Intent(this, FarmerProductsActivity.class));
        else if (id == R.id.action_orders) startActivity(new Intent(this, OrdersActivity.class).putExtra("farmer", true));
        else if (id == R.id.action_logout) Ui.logout(this);
        return true;
    }
}

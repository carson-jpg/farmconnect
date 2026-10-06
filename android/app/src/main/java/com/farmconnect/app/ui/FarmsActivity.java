package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.data.Models.FarmRequest;
import com.farmconnect.app.databinding.ActivityListBinding;
import com.farmconnect.app.databinding.DialogFarmBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;

public class FarmsActivity extends AppCompatActivity implements FarmAdapter.Listener {
    private ActivityListBinding b;
    private FarmerViewModel vm;
    private FarmAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("My farms");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adapter = new FarmAdapter(this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.tvEmpty.setText("No farms yet. Tap + to add your first farm.");
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> showDialog(null));
        load();
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        Ui.watch(this, vm.myFarms(), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showDialog(Farm ex) {
        DialogFarmBinding d = DialogFarmBinding.inflate(getLayoutInflater());
        if (ex != null) {
            d.etName.setText(ex.name);
            d.etLocation.setText(ex.location);
            d.etDesc.setText(ex.description);
        }
        new AlertDialog.Builder(this)
                .setTitle(ex == null ? "New farm" : "Edit farm")
                .setView(d.getRoot())
                .setPositiveButton("Save", (x, y) -> {
                    String name = d.etName.getText().toString().trim();
                    if (name.isEmpty()) { Ui.toast(this, "Farm name required"); return; }
                    FarmRequest r = new FarmRequest(name, d.etLocation.getText().toString().trim(),
                            d.etDesc.getText().toString().trim());
                    if (ex == null) Ui.watch(this, vm.createFarm(r), b.progress, f -> load());
                    else Ui.watch(this, vm.updateFarm(ex.id, r), b.progress, f -> load());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onEdit(Farm f) { showDialog(f); }

    @Override public void onDelete(Farm f) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + f.name + "?")
                .setMessage("A farm with products cannot be deleted. Remove its products first.")
                .setPositiveButton("Delete", (x, y) -> Ui.watch(this, vm.deleteFarm(f.id), b.progress, r -> {
                    Ui.toast(this, "Farm deleted");
                    load();
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.databinding.ActivityAdminProductsBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AdminViewModel;

/** Admin product moderation: see every product (even from unverified farmers) and take down / restore listings. */
public class AdminProductsActivity extends AppCompatActivity implements ProductAdapter.Listener {
    private ActivityAdminProductsBinding b;
    private AdminViewModel vm;
    private ProductAdapter adapter;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAdminProductsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(AdminViewModel.class);
        b.tvTitle.setText("Product moderation");
        b.btnBack.setOnClickListener(v -> finish());
        adapter = new ProductAdapter(ProductAdapter.Mode.ADMIN, this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.etSearch.setOnEditorActionListener((v, a, e) -> { load(); return true; });
        load();
    }

    private void load() {
        Ui.watch(this, vm.products(b.etSearch.getText().toString().trim()), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            int down = 0;
            for (Product p : list) if (!p.active) down++;
            b.tvSubtitle.setText(list.size() + " listings · " + down + " taken down");
        });
    }

    @Override public void onPrimary(Product p) {
        boolean makeActive = !p.active;
        new AlertDialog.Builder(this)
                .setTitle((makeActive ? "Restore " : "Take down ") + p.name + "?")
                .setMessage(makeActive ? "It will be visible to buyers again (if the farmer is verified)."
                        : "Buyers will no longer see this product. The farmer keeps it and can still edit it.")
                .setPositiveButton(makeActive ? "Restore" : "Take down", (d, w) ->
                        Ui.watch(this, vm.setProductActive(p.id, makeActive), b.progress, x -> load()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public void onSecondary(Product p) {}
}

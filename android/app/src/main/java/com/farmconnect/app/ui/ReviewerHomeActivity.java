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
import com.farmconnect.app.data.Models.ReviewSummary;
import com.farmconnect.app.databinding.ActivityReviewListBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.ReviewViewModel;
import com.google.android.material.chip.Chip;

/** Home screen for ADMIN and County Agricultural Officer accounts. */
public class ReviewerHomeActivity extends AppCompatActivity implements ReviewAdapter.Listener {
    private ActivityReviewListBinding b;
    private ReviewViewModel vm;
    private ReviewAdapter adapter;
    private String status = "PENDING";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityReviewListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(ReviewViewModel.class);
        b.btnLogout.setOnClickListener(v -> Ui.logout(this));
        adapter = new ReviewAdapter(this);
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            status = String.valueOf(c.getTag());
            load();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Ui.watch(this, vm.list(status), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onOpen(ReviewSummary r) {
        startActivity(new Intent(this, ReviewDetailActivity.class).putExtra("id", r.id));
    }

    @Override public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.menu_reviewer, m);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem i) {
        if (i.getItemId() == R.id.action_logout) Ui.logout(this);
        return true;
    }
}
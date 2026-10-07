package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.OfficerRequest;
import com.farmconnect.app.data.Models.UserSummary;
import com.farmconnect.app.databinding.ActivityAdminUsersBinding;
import com.farmconnect.app.databinding.DialogOfficerBinding;
import com.farmconnect.app.databinding.ItemUserBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AdminViewModel;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

/** Admin: search every account, filter by role, suspend / re-activate, create county officer accounts. */
public class AdminUsersActivity extends AppCompatActivity {
    private ActivityAdminUsersBinding b;
    private AdminViewModel vm;
    private UserAdapter adapter;
    private String role = "ALL";

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityAdminUsersBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(AdminViewModel.class);
        b.tvTitle.setText("Users");
        b.btnBack.setOnClickListener(v -> finish());
        adapter = new UserAdapter();
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.etSearch.setOnEditorActionListener((v, a, e) -> { load(); return true; });
        b.chipGroup.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            role = String.valueOf(((Chip) g.findViewById(ids.get(0))).getTag());
            load();
        });
        b.fabOfficer.setOnClickListener(v -> officerDialog());
        load();
    }

    private void load() {
        Ui.watch(this, vm.users("ALL".equals(role) ? null : role, b.etSearch.getText().toString().trim()), b.progress, list -> {
            adapter.set(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            b.tvSubtitle.setText(list.size() + (list.size() == 1 ? " account" : " accounts"));
        });
    }

    private void toggle(UserSummary u) {
        boolean enable = !u.enabled;
        new AlertDialog.Builder(this)
                .setTitle((enable ? "Re-activate " : "Suspend ") + u.name + "?")
                .setMessage(enable ? "They will be able to log in again."
                        : "They will be logged out and blocked until you re-activate them.")
                .setPositiveButton(enable ? "Re-activate" : "Suspend", (d, w) ->
                        Ui.watch(this, vm.setUserEnabled(u.id, enable), b.progress, x -> {
                            Ui.toast(this, enable ? "Account re-activated" : "Account suspended");
                            load();
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void officerDialog() {
        DialogOfficerBinding d = DialogOfficerBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New county officer")
                .setView(d.getRoot())
                .setPositiveButton("Create account", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = d.etName.getText().toString().trim();
            String email = d.etEmail.getText().toString().trim();
            String phone = d.etPhone.getText().toString().trim();
            String pass = d.etPassword.getText().toString();
            if (name.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    || phone.length() < 9 || pass.length() < 8) {
                Ui.toast(this, "Fill every field. The password needs at least 8 characters.");
                return;
            }
            Ui.watch(this, vm.createOfficer(new OfficerRequest(name, email, phone, pass)), b.progress, r -> {
                dialog.dismiss();
                Ui.toast(this, "Officer account created");
                load();
            });
        });
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private List<UserSummary> items = new ArrayList<>();

        void set(List<UserSummary> l) { items = l; notifyDataSetChanged(); }

        class VH extends RecyclerView.ViewHolder {
            final ItemUserBinding ib;
            VH(ItemUserBinding ib) { super(ib.getRoot()); this.ib = ib; }
        }

        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(ItemUserBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            UserSummary u = items.get(pos);
            h.ib.tvAvatar.setText(Ui.initial(u.name));
            h.ib.tvName.setText(u.name);
            h.ib.tvEmail.setText(u.email);
            StringBuilder tags = new StringBuilder(u.role.charAt(0) + u.role.substring(1).toLowerCase());
            if ("FARMER".equals(u.role)) tags.append(u.verified ? "  ·  ✓ Verified" : "  ·  Not verified");
            if (!u.enabled) tags.append("  ·  ⛔ Suspended");
            h.ib.tvTags.setText(tags.toString());
            h.ib.tvTags.setTextColor(u.enabled ? 0xFF2E7D32 : 0xFFC62828);
            boolean admin = "ADMIN".equals(u.role);
            h.ib.btnToggle.setVisibility(admin ? View.GONE : View.VISIBLE);
            h.ib.btnToggle.setText(u.enabled ? "Suspend" : "Re-activate");
            h.ib.btnToggle.setTextColor(u.enabled ? 0xFFC62828 : 0xFF2E7D32);
            h.ib.btnToggle.setOnClickListener(v -> toggle(u));
        }

        @Override public int getItemCount() { return items.size(); }
    }
}

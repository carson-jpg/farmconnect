package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.AssignmentRequest;
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
        b.tvTitle.setText(I18n.t("Users"));
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
            b.tvSubtitle.setText(list.size() + (list.size() == 1 ? I18n.t(" account") : I18n.t(" accounts")));
        });
    }

    private void toggle(UserSummary u) {
        boolean enable = !u.enabled;
        new AlertDialog.Builder(this)
                .setTitle((enable ? I18n.t("Re-activate ") : I18n.t("Suspend ")) + u.name + "?")
                .setMessage(enable ? I18n.t("They will be able to log in again.")
                        : I18n.t("They will be logged out and blocked until you re-activate them."))
                .setPositiveButton(enable ? I18n.t("Re-activate") : I18n.t("Suspend"), (d, w) ->
                        Ui.watch(this, vm.setUserEnabled(u.id, enable), b.progress, x -> {
                            Ui.toast(this, enable ? I18n.t("Account re-activated") : I18n.t("Account suspended"));
                            load();
                        }))
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    private String areaText(UserSummary u) {
        if (u.assignedCounty == null || u.assignedCounty.isEmpty()) return I18n.t("All counties");
        if (u.assignedSubCounties == null || u.assignedSubCounties.isEmpty()) return u.assignedCounty + " (" + I18n.t("Whole county") + ")";
        return u.assignedCounty + ": " + android.text.TextUtils.join(", ", u.assignedSubCounties);
    }

    /** Admin: pick the county an officer works in, then which of its sub-counties (none ticked = the whole county). */
    private void assignArea(UserSummary u) {
        String[] counties = new String[Labels.COUNTIES.length + 1];
        counties[0] = I18n.t("All counties (no restriction)");
        System.arraycopy(Labels.COUNTIES, 0, counties, 1, Labels.COUNTIES.length);
        new AlertDialog.Builder(this).setTitle(I18n.t("Area for ") + u.name)
                .setItems(counties, (d, which) -> {
                    if (which == 0) { saveArea(u, null, new ArrayList<>()); return; }
                    String county = Labels.COUNTIES[which - 1];
                    String[] subs = Labels.SUBS[which - 1];
                    boolean[] on = new boolean[subs.length];
                    if (county.equals(u.assignedCounty) && u.assignedSubCounties != null)
                        for (int i = 0; i < subs.length; i++) on[i] = u.assignedSubCounties.contains(subs[i]);
                    new AlertDialog.Builder(this).setTitle(county + " · " + I18n.t("Sub-counties"))
                            .setMultiChoiceItems(subs, on, (dd, i, checked) -> on[i] = checked)
                            .setPositiveButton(I18n.t("Save"), (dd, w) -> {
                                List<String> chosen = new ArrayList<>();
                                for (int i = 0; i < subs.length; i++) if (on[i]) chosen.add(subs[i]);
                                saveArea(u, county, chosen);
                            })
                            .setNegativeButton(I18n.t("Cancel"), null)
                            .show();
                })
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    private void saveArea(UserSummary u, String county, List<String> subs) {
        Ui.watch(this, vm.assignOfficer(u.id, new AssignmentRequest(county, subs)), b.progress, x -> {
            Ui.toast(this, I18n.t("Area saved"));
            load();
        });
    }

    private void officerDialog() {
        DialogOfficerBinding d = DialogOfficerBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(I18n.t("New county officer"))
                .setView(d.getRoot())
                .setPositiveButton(I18n.t("Create account"), null)
                .setNegativeButton(I18n.t("Cancel"), null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = d.etName.getText().toString().trim();
            String email = d.etEmail.getText().toString().trim();
            String phone = d.etPhone.getText().toString().trim();
            String pass = d.etPassword.getText().toString();
            if (name.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    || phone.length() < 9 || pass.length() < 8) {
                Ui.toast(this, I18n.t("Fill every field. The password needs at least 8 characters."));
                return;
            }
            Ui.watch(this, vm.createOfficer(new OfficerRequest(name, email, phone, pass)), b.progress, r -> {
                dialog.dismiss();
                Ui.toast(this, I18n.t("Officer account created"));
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
            if ("FARMER".equals(u.role)) tags.append(u.verified ? I18n.t("  ·  ✓ Verified") : I18n.t("  ·  Not verified"));
            if ("OFFICER".equals(u.role)) tags.append("  ·  ").append(areaText(u));
            if (!u.enabled) tags.append(I18n.t("  ·  ⛔ Suspended"));
            h.ib.tvTags.setText(tags.toString());
            h.ib.tvTags.setTextColor(u.enabled ? 0xFF2E7D32 : 0xFFC62828);
            boolean admin = "ADMIN".equals(u.role);
            h.ib.btnToggle.setVisibility(admin ? View.GONE : View.VISIBLE);
            h.ib.btnToggle.setText(u.enabled ? I18n.t("Suspend") : I18n.t("Re-activate"));
            h.ib.btnToggle.setTextColor(u.enabled ? 0xFFC62828 : 0xFF2E7D32);
            h.ib.btnToggle.setOnClickListener(v -> toggle(u));
            // tap an officer to choose the county and sub-counties they cover
            h.ib.getRoot().setOnClickListener("OFFICER".equals(u.role) ? v -> assignArea(u) : null);
        }

        @Override public int getItemCount() { return items.size(); }
    }
}

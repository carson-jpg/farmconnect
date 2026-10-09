package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.farmconnect.app.databinding.ActivityListPageBinding;
import com.farmconnect.app.vm.CommunityViewModel;
import com.google.android.material.chip.Chip;
import java.util.List;

/**
 * Header + optional search + optional filter chips + list of {@link Row}s + optional floating button.
 * Subclasses only say what to show; the page refreshes in onResume so it is always current after returning.
 */
public abstract class BaseListActivity extends AppCompatActivity {
    protected ActivityListPageBinding b;
    protected CommunityViewModel vm;
    protected RowAdapter adapter;
    /** Tag of the selected filter chip ("" when there are no chips). */
    protected String chip = "";

    protected abstract String screenTitle();
    protected abstract void load();

    /** {tag, label} pairs. The first one is selected at start. */
    protected String[][] chips() { return null; }
    protected boolean searchable() { return false; }
    protected String searchHint() { return I18n.t("Search"); }
    protected String fabText() { return null; }
    protected void onFab() {}
    protected String emptyText() { return I18n.t("Nothing here yet"); }
    protected String headerActionLabel() { return null; }
    protected void onHeaderAction() {}

    protected String query() { return b.etSearch.getText().toString().trim(); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityListPageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        b.tvTitle.setText(screenTitle());
        b.btnBack.setOnClickListener(v -> finish());
        adapter = new RowAdapter();
        b.rv.setLayoutManager(new LinearLayoutManager(this));
        b.rv.setAdapter(adapter);
        b.tvEmpty.setText(emptyText());

        if (searchable()) {
            b.searchCard.setVisibility(View.VISIBLE);
            b.etSearch.setHint(searchHint());
            b.etSearch.setOnEditorActionListener((v, a, e) -> { load(); return true; });
        }
        String[][] cs = chips();
        if (cs != null) buildChips(cs);
        String fab = fabText();
        if (fab != null) {
            b.fab.setVisibility(View.VISIBLE);
            b.fab.setText(fab);
            b.fab.setOnClickListener(v -> onFab());
        }
        String ha = headerActionLabel();
        if (ha != null) {
            b.btnHeaderAction.setVisibility(View.VISIBLE);
            b.btnHeaderAction.setText(ha);
            b.btnHeaderAction.setOnClickListener(v -> onHeaderAction());
        }
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void buildChips(String[][] cs) {
        b.chipScroll.setVisibility(View.VISIBLE);
        ColorStateList bg = new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{0xFF2E7D32, 0xFFFFFFFF});
        ColorStateList fg = new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{0xFFFFFFFF, 0xFF1C2B1E});
        for (int i = 0; i < cs.length; i++) {
            Chip c = new Chip(this);
            c.setId(View.generateViewId());
            c.setText(com.farmconnect.app.util.I18n.t(cs[i][1]));
            c.setTag(cs[i][0]);
            c.setCheckable(true);
            c.setCheckedIconVisible(false);
            c.setChipBackgroundColor(bg);
            c.setTextColor(fg);
            c.setChipStrokeColor(ColorStateList.valueOf(0xFFD5E3D0));
            c.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            c.setChecked(i == 0);
            b.chipGroup.addView(c);
        }
        chip = cs[0][0];
        b.chipGroup.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            chip = String.valueOf(((Chip) g.findViewById(ids.get(0))).getTag());
            load();
        });
    }

    /** Show rows (and an optional subtitle under the title). */
    protected void show(List<Row> rows, String subtitle) {
        adapter.set(rows);
        b.tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvSubtitle.setText(subtitle == null ? "" : subtitle);
    }

    protected int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}

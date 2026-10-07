package com.farmconnect.app.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.databinding.ItemRowBinding;
import java.util.ArrayList;
import java.util.List;

public class RowAdapter extends RecyclerView.Adapter<RowAdapter.VH> {
    private List<Row> rows = new ArrayList<>();

    public void set(List<Row> list) { rows = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRowBinding b;
        VH(ItemRowBinding b) { super(b.getRoot()); this.b = b; }
    }

    @Override public VH onCreateViewHolder(ViewGroup p, int t) {
        return new VH(ItemRowBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        Row r = rows.get(pos);
        h.b.tvIcon.setText(r.icon);
        h.b.tvTitle.setText(r.title);
        show(h.b.tvSubtitle, r.subtitle);
        show(h.b.tvMeta, r.meta);
        show(h.b.tvBadge, r.badge);
        if (r.badge != null && !r.badge.isEmpty()) {
            h.b.tvBadge.setTextColor(r.badgeColor);
            h.b.tvBadge.setBackgroundTintList(ColorStateList.valueOf((r.badgeColor & 0x00FFFFFF) | 0x22000000));
        }
        h.b.dotUnread.setVisibility(r.unread ? View.VISIBLE : View.GONE);
        h.b.tvTitle.setTypeface(null, r.unread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.BOLD);
        boolean act = r.action != null && !r.action.isEmpty();
        h.b.btnAction.setVisibility(act ? View.VISIBLE : View.GONE);
        if (act) {
            h.b.btnAction.setText(r.action);
            h.b.btnAction.setOnClickListener(v -> { if (r.onAction != null) r.onAction.run(); });
        }
        h.itemView.setOnClickListener(v -> { if (r.onClick != null) r.onClick.run(); });
        h.itemView.setClickable(r.onClick != null);
    }

    private static void show(android.widget.TextView t, String s) {
        boolean has = s != null && !s.trim().isEmpty();
        t.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) t.setText(s);
    }

    @Override public int getItemCount() { return rows.size(); }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.databinding.ItemFarmBinding;
import java.util.ArrayList;
import java.util.List;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.VH> {
    public interface Listener {
        void onEdit(Farm f);
        void onDelete(Farm f);
    }

    private final Listener listener;
    private List<Farm> items = new ArrayList<>();

    public FarmAdapter(Listener l) { listener = l; }

    public void set(List<Farm> list) { items = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFarmBinding b;
        VH(ItemFarmBinding b) { super(b.getRoot()); this.b = b; }
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
        return new VH(ItemFarmBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        Farm f = items.get(pos);
        h.b.tvName.setText(f.name);
        h.b.tvLocation.setText(f.location == null || f.location.isEmpty() ? I18n.t("No location set") : "📍 " + f.location + (f.subCounty == null || f.subCounty.isEmpty() ? "" : " · " + f.subCounty));
        h.b.tvDesc.setText(f.description == null ? "" : f.description);
        h.b.btnEdit.setOnClickListener(v -> listener.onEdit(f));
        h.b.btnDelete.setOnClickListener(v -> listener.onDelete(f));
    }

    @Override public int getItemCount() { return items.size(); }
}
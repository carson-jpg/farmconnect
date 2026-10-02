package com.farmconnect.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.databinding.ItemFarmBinding;
import java.util.ArrayList;
import java.util.List;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.VH> {
    private List<Farm> items = new ArrayList<>();

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
        h.b.tvLocation.setText(f.location == null ? "" : f.location);
        h.b.tvDesc.setText(f.description == null ? "" : f.description);
    }

    @Override public int getItemCount() { return items.size(); }
}

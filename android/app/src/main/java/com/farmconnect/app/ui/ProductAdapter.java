package com.farmconnect.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.databinding.ItemProductBinding;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
    public enum Mode { BUYER, WISHLIST, FARMER }

    public interface Listener {
        void onPrimary(Product p);
        void onSecondary(Product p);
    }

    private final Mode mode;
    private final Listener listener;
    private List<Product> items = new ArrayList<>();

    public ProductAdapter(Mode mode, Listener listener) { this.mode = mode; this.listener = listener; }

    public void set(List<Product> list) { items = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemProductBinding b;
        VH(ItemProductBinding b) { super(b.getRoot()); this.b = b; }
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
        return new VH(ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        Product p = items.get(pos);
        h.b.tvName.setText(p.name);
        h.b.tvInfo.setText(p.farmName + (p.category != null && !p.category.isEmpty() ? " · " + p.category : ""));
        h.b.tvPrice.setText(Ui.kes(p.price) + (p.unit != null && !p.unit.isEmpty() ? " / " + p.unit : ""));
        h.b.tvStock.setText(p.quantity > 0 ? p.quantity + " in stock" : "Out of stock");
        h.b.btnSecondary.setVisibility(View.VISIBLE);
        h.b.btnPrimary.setEnabled(true);
        switch (mode) {
            case BUYER:
                h.b.btnPrimary.setText("Add to cart");
                h.b.btnPrimary.setEnabled(p.quantity > 0);
                h.b.btnSecondary.setText("♡ Save");
                break;
            case WISHLIST:
                h.b.btnPrimary.setText("Add to cart");
                h.b.btnPrimary.setEnabled(p.quantity > 0);
                h.b.btnSecondary.setText("Remove");
                break;
            case FARMER:
                h.b.btnPrimary.setText("Delete");
                h.b.btnSecondary.setVisibility(View.GONE);
                break;
        }
        h.b.btnPrimary.setOnClickListener(v -> listener.onPrimary(p));
        h.b.btnSecondary.setOnClickListener(v -> listener.onSecondary(p));
    }

    @Override public int getItemCount() { return items.size(); }
}

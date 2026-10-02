package com.farmconnect.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.CartItem;
import com.farmconnect.app.databinding.ItemCartBinding;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
    public interface Listener {
        void onPlus(CartItem i);
        void onMinus(CartItem i);
        void onRemove(CartItem i);
    }

    private final Listener listener;
    private List<CartItem> items = new ArrayList<>();

    public CartAdapter(Listener l) { listener = l; }

    public void set(List<CartItem> list) { items = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemCartBinding b;
        VH(ItemCartBinding b) { super(b.getRoot()); this.b = b; }
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
        return new VH(ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        CartItem c = items.get(pos);
        h.b.tvName.setText(c.product.name);
        h.b.tvLine.setText(Ui.kes(c.product.price) + " x " + c.quantity + " = " + Ui.kes(c.product.price * c.quantity));
        h.b.tvQty.setText(String.valueOf(c.quantity));
        h.b.btnPlus.setOnClickListener(v -> listener.onPlus(c));
        h.b.btnMinus.setOnClickListener(v -> listener.onMinus(c));
        h.b.btnRemove.setOnClickListener(v -> listener.onRemove(c));
    }

    @Override public int getItemCount() { return items.size(); }
}

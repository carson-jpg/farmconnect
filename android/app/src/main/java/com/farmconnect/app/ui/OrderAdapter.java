package com.farmconnect.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Models.OrderItem;
import com.farmconnect.app.databinding.ItemOrderBinding;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {
    public interface Listener { void onAction(Order o); }

    private final boolean farmer;
    private final Listener listener;
    private List<Order> items = new ArrayList<>();

    public OrderAdapter(boolean farmer, Listener l) { this.farmer = farmer; listener = l; }

    public void set(List<Order> list) { items = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    public static String next(String s) {
        switch (s) {
            case "PENDING": return "CONFIRMED";
            case "CONFIRMED": return "SHIPPED";
            case "SHIPPED": return "DELIVERED";
            default: return null;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemOrderBinding b;
        VH(ItemOrderBinding b) { super(b.getRoot()); this.b = b; }
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
        return new VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        Order o = items.get(pos);
        h.b.tvHeader.setText("Order #" + o.id);
        Ui.styleStatus(h.b.tvStatus, o.status);
        h.b.tvDate.setText(Ui.dateTime(o.createdAt));

        if (farmer) {
            String who = o.buyerName == null || o.buyerName.isEmpty() ? "Customer" : o.buyerName;
            if (o.buyerPhone != null && !o.buyerPhone.isEmpty()) who += " · " + o.buyerPhone;
            h.b.tvBuyer.setText("👤 " + who);
            h.b.tvBuyer.setVisibility(View.VISIBLE);
        } else {
            h.b.tvBuyer.setVisibility(View.GONE);
        }

        StringBuilder sb = new StringBuilder();
        for (OrderItem i : o.items) sb.append(i.quantity).append(" x ").append(i.productName).append("\n");
        h.b.tvItems.setText(sb.toString().trim());
        h.b.tvAddress.setText("📍 " + o.deliveryAddress);
        h.b.tvTotal.setText("Total: " + Ui.kes(o.total));

        if (farmer) {
            String n = next(o.status);
            boolean show = n != null && !"CANCELLED".equals(o.status);
            h.b.btnAction.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) h.b.btnAction.setText("Mark as " + n.charAt(0) + n.substring(1).toLowerCase());
        } else {
            h.b.btnAction.setVisibility("PENDING".equals(o.status) ? View.VISIBLE : View.GONE);
            h.b.btnAction.setText("Cancel order");
        }
        h.b.btnAction.setOnClickListener(v -> listener.onAction(o));
    }

    @Override public int getItemCount() { return items.size(); }
}
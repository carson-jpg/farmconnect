package com.farmconnect.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Models.OrderItem;
import com.farmconnect.app.databinding.ItemOrderBinding;
import com.farmconnect.app.util.Ui;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/** One order card for buyers, farmers and admins. Tapping a card opens the full order page. */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {
    public enum Mode { BUYER, FARMER, ADMIN }

    public interface Listener {
        void onAction(Order o);
        default void onSecondary(Order o) {}
    }

    private final Mode mode;
    private final Listener listener;
    private List<Order> items = new ArrayList<>();

    public OrderAdapter(Mode mode, Listener l) { this.mode = mode; listener = l; }

    public void set(List<Order> list) { items = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    public static String next(String s) {
        switch (s) {
            case "PENDING": return "CONFIRMED";
            case "CONFIRMED": return "SHIPPED";
            case "SHIPPED": return "DELIVERED";
            default: return null;
        }
    }

    /** 0..3 for PENDING..DELIVERED, -1 for CANCELLED. */
    public static int step(String s) {
        switch (s) {
            case "PENDING": return 0;
            case "CONFIRMED": return 1;
            case "SHIPPED": return 2;
            case "DELIVERED": return 3;
            default: return -1;
        }
    }

    public static String actionLabel(String status) {
        String n = next(status);
        if (n == null) return null;
        switch (n) {
            case "CONFIRMED": return "Confirm order";
            case "SHIPPED": return "Mark as shipped";
            default: return "Mark as delivered";
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
        Context ctx = h.itemView.getContext();
        h.b.tvHeader.setText("Order #" + o.id);
        Ui.styleStatus(h.b.tvStatus, o.status);
        h.b.tvDate.setText(Ui.dateTime(o.createdAt));

        if (mode != Mode.BUYER) {
            String who = o.buyerName == null || o.buyerName.isEmpty() ? "Customer" : o.buyerName;
            String phone = o.deliveryPhone != null && !o.deliveryPhone.isEmpty() ? o.deliveryPhone : o.buyerPhone;
            if (phone != null && !phone.isEmpty()) who += " · " + phone;
            h.b.tvBuyer.setText("👤 " + who);
            h.b.tvBuyer.setVisibility(View.VISIBLE);
        } else {
            h.b.tvBuyer.setVisibility(View.GONE);
        }

        // progress tracker
        int step = step(o.status);
        View[] segs = {h.b.t1, h.b.t2, h.b.t3, h.b.t4};
        for (int i = 0; i < 4; i++) {
            int color = step < 0 ? 0xFFE57373 : (i <= step ? 0xFF2E7D32 : 0xFFD5E3D0);
            segs[i].setBackgroundTintList(ColorStateList.valueOf(color));
        }

        // thumbnails (max 4) + text summary
        h.b.thumbs.removeAllViews();
        int shown = Math.min(4, o.items.size());
        for (int i = 0; i < shown; i++) h.b.thumbs.addView(Thumbs.make(ctx, o.items.get(i).imageUrl, 46, 8));
        if (o.items.size() > shown) {
            TextView more = new TextView(ctx);
            more.setText("+" + (o.items.size() - shown));
            more.setTextColor(0xFF5E7061);
            more.setTextSize(13);
            h.b.thumbs.addView(more);
        }
        StringBuilder sb = new StringBuilder();
        int qty = 0;
        for (OrderItem i : o.items) { qty += i.quantity; if (sb.length() > 0) sb.append(", "); sb.append(i.quantity).append(" × ").append(i.productName); }
        h.b.tvItems.setText(sb.toString());
        h.b.tvAddress.setText("📍 " + o.deliveryAddress);
        h.b.tvTotal.setText(Ui.kes(o.total));

        // actions
        boolean showAction = false, showSecondary = false;
        String actionText = null;
        if (mode == Mode.BUYER) {
            showAction = "PENDING".equals(o.status);
            actionText = "Cancel order";
        } else {
            actionText = actionLabel(o.status);
            showAction = actionText != null;
            showSecondary = mode == Mode.ADMIN && step >= 0 && step < 3;
        }
        h.b.actions.setVisibility(showAction || showSecondary ? View.VISIBLE : View.GONE);
        h.b.btnAction.setVisibility(showAction ? View.VISIBLE : View.GONE);
        if (showAction) h.b.btnAction.setText(actionText);
        h.b.btnSecondary.setVisibility(showSecondary ? View.VISIBLE : View.GONE);
        h.b.btnAction.setOnClickListener(v -> listener.onAction(o));
        h.b.btnSecondary.setOnClickListener(v -> listener.onSecondary(o));

        h.itemView.setOnClickListener(v -> ctx.startActivity(new Intent(ctx, OrderDetailActivity.class)
                .putExtra("order", new Gson().toJson(o)).putExtra("mode", mode.name())));
    }

    @Override public int getItemCount() { return items.size(); }
}

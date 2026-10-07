package com.farmconnect.app.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.farmconnect.app.R;
import com.farmconnect.app.data.ApiClient;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.databinding.ItemProductBinding;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
    public enum Mode { BUYER, WISHLIST, FARMER, ADMIN }

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
        Context ctx = h.itemView.getContext();

        // cover photo
        String cover = p.imageUrls != null && !p.imageUrls.isEmpty() ? p.imageUrls.get(0) : p.imageUrl;
        if (cover != null && !cover.isEmpty()) {
            Glide.with(ctx).load(ApiClient.absolute(cover)).centerCrop()
                    .placeholder(R.drawable.ph_product).error(R.drawable.ph_product).into(h.b.ivCover);
        } else {
            Glide.with(ctx).clear(h.b.ivCover);
            h.b.ivCover.setImageResource(R.drawable.ph_product);
        }
        int photos = p.imageUrls == null ? 0 : p.imageUrls.size();
        h.b.tvPhotoCount.setVisibility(photos > 1 ? View.VISIBLE : View.GONE);
        h.b.tvPhotoCount.setText("\uD83D\uDCF7 " + photos);
        h.b.tvVerified.setVisibility(p.farmerVerified ? View.VISIBLE : View.GONE);
        String hidden = null;
        if (!p.active) hidden = mode == Mode.ADMIN ? "⛔ Taken down by admin" : "⛔ Removed by an administrator";
        else if (!p.farmerVerified && mode == Mode.FARMER) hidden = "⏳ Hidden from buyers until you are verified";
        else if (!p.farmerVerified && mode == Mode.ADMIN) hidden = "⏳ Farmer not verified yet (not public)";
        h.b.tvHidden.setVisibility(hidden != null ? View.VISIBLE : View.GONE);
        if (hidden != null) h.b.tvHidden.setText(hidden);

        h.b.tvName.setText(p.name);
        boolean hasSummary = p.summary != null && !p.summary.trim().isEmpty();
        h.b.tvSummary.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        if (hasSummary) h.b.tvSummary.setText(p.summary);

        StringBuilder info = new StringBuilder();
        if (p.farmName != null) info.append(p.farmName);
        if (p.farmLocation != null && !p.farmLocation.isEmpty()) info.append(" · ").append(p.farmLocation);
        if (p.category != null && !p.category.isEmpty()) info.append(" · ").append(p.category);
        h.b.tvInfo.setText(info.toString());

        h.b.tvPrice.setText(Ui.kes(p.price) + (p.unit != null && !p.unit.isEmpty() ? " / " + p.unit : ""));
        h.b.tvStock.setText(p.quantity > 0 ? p.quantity + " in stock" : "Out of stock");
        h.b.tvStock.setTextColor(p.quantity <= 0 || (mode == Mode.FARMER && p.quantity <= 5) ? 0xFFC62828 : 0xFF2E7D32);
        h.b.btnSecondary.setVisibility(View.VISIBLE);
        h.b.btnPrimary.setEnabled(true);
        switch (mode) {
            case BUYER:
                h.b.btnPrimary.setText("Add to cart");
                h.b.btnPrimary.setEnabled(p.quantity > 0);
                h.b.btnSecondary.setText("\u2661 Save");
                break;
            case WISHLIST:
                h.b.btnPrimary.setText("Add to cart");
                h.b.btnPrimary.setEnabled(p.quantity > 0);
                h.b.btnSecondary.setText("Remove");
                break;
            case ADMIN:
                h.b.btnPrimary.setText(p.active ? "Take down" : "Restore");
                h.b.btnSecondary.setVisibility(View.GONE);
                break;
            case FARMER:
                h.b.btnPrimary.setText("Edit");
                h.b.btnSecondary.setText("Delete");
                h.b.btnSecondary.setTextColor(0xFFC62828);
                break;
        }
        h.b.btnPrimary.setOnClickListener(v -> listener.onPrimary(p));
        h.b.btnSecondary.setOnClickListener(v -> listener.onSecondary(p));
        // tapping the card opens the full product page (gallery + full description)
        h.itemView.setOnClickListener(v ->
                ctx.startActivity(new Intent(ctx, ProductDetailActivity.class).putExtra("id", p.id)));
    }

    @Override public int getItemCount() { return items.size(); }
}
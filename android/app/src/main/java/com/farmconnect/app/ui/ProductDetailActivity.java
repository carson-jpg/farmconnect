package com.farmconnect.app.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.farmconnect.app.R;
import com.farmconnect.app.data.ApiClient;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityProductDetailBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import java.util.ArrayList;
import java.util.List;

/** Full product page: swipeable photo gallery, full description, seller, quantity + add to cart. */
public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding b;
    private BuyerViewModel vm;
    private Product product;
    private int qty = 1;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);

        b.btnBack.setOnClickListener(v -> finish());
        // only buyers can purchase; farmers / reviewers just preview
        b.buyBar.setVisibility("BUYER".equals(Session.role()) ? View.VISIBLE : View.GONE);

        b.btnMinus.setOnClickListener(v -> { if (qty > 1) { qty--; b.tvQty.setText(String.valueOf(qty)); } });
        b.btnPlus.setOnClickListener(v -> {
            if (product != null && qty < product.quantity) { qty++; b.tvQty.setText(String.valueOf(qty)); }
            else Ui.toast(this, "That is all we have in stock");
        });
        b.btnCart.setOnClickListener(v -> {
            if (product == null) return;
            Ui.watch(this, vm.addToCart(product.id, qty), null, c -> Ui.toast(this, "Added " + qty + " to cart"));
        });
        b.btnSave.setOnClickListener(v -> {
            if (product == null) return;
            Ui.watch(this, vm.addWish(product.id), null, x -> Ui.toast(this, "Saved to wishlist"));
        });

        long id = getIntent().getLongExtra("id", -1);
        if (id < 0) { finish(); return; }
        Ui.watch(this, vm.product(id), b.progress, this::show);
    }

    private void show(Product p) {
        product = p;
        b.tvName.setText(p.name);
        b.tvPrice.setText(Ui.kes(p.price) + (p.unit != null && !p.unit.isEmpty() ? " / " + p.unit : ""));
        b.tvStock.setText(p.quantity > 0 ? p.quantity + " in stock" : "Out of stock");
        b.tvStock.setTextColor(p.quantity > 0 ? 0xFF2E7D32 : 0xFFC62828);
        b.btnCart.setEnabled(p.quantity > 0);
        b.tvVerified.setVisibility(p.farmerVerified ? View.VISIBLE : View.GONE);

        boolean hasSummary = p.summary != null && !p.summary.trim().isEmpty();
        b.tvSummary.setVisibility(hasSummary ? View.VISIBLE : View.GONE);
        if (hasSummary) b.tvSummary.setText(p.summary);

        boolean hasDesc = p.description != null && !p.description.trim().isEmpty();
        b.tvDescription.setText(hasDesc ? p.description : "The farmer has not added a description yet.");
        b.tvDescription.setAlpha(hasDesc ? 1f : 0.6f);

        boolean hasCat = p.category != null && !p.category.isEmpty();
        b.tvCategory.setVisibility(hasCat ? View.VISIBLE : View.GONE);
        if (hasCat) b.tvCategory.setText(p.category);

        boolean buyer = "BUYER".equals(Session.role());
        b.btnMessageSeller.setVisibility(buyer && p.farmerId > 0 ? View.VISIBLE : View.GONE);
        b.btnMessageSeller.setOnClickListener(v -> startActivity(new android.content.Intent(this, ChatActivity.class)
                .putExtra("userId", p.farmerId).putExtra("name", p.farmName == null ? "Seller" : p.farmName)));
        b.tvFarm.setText(p.farmName == null ? "" : p.farmName + (p.farmerVerified ? "  ✓" : ""));
        b.tvFarmLoc.setText(p.farmLocation == null || p.farmLocation.isEmpty() ? "Trans Nzoia County" : p.farmLocation);

        List<String> urls = new ArrayList<>();
        if (p.imageUrls != null) urls.addAll(p.imageUrls);
        if (urls.isEmpty() && p.imageUrl != null && !p.imageUrl.isEmpty()) urls.add(p.imageUrl);
        b.pager.setAdapter(new GalleryAdapter(urls));
        if (urls.size() > 1) {
            b.tvCount.setVisibility(View.VISIBLE);
            b.tvCount.setText("1 / " + urls.size());
            b.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int position) {
                    b.tvCount.setText((position + 1) + " / " + urls.size());
                }
            });
        }
    }

    private static class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<String> urls;
        GalleryAdapter(List<String> urls) { this.urls = urls; }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new RecyclerView.ViewHolder(iv) { };
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            ImageView iv = (ImageView) h.itemView;
            if (urls.isEmpty()) { iv.setImageResource(R.drawable.ph_product); return; }
            Glide.with(iv).load(ApiClient.absolute(urls.get(pos))).centerCrop()
                    .placeholder(R.drawable.ph_product).error(R.drawable.ph_product).into(iv);
        }

        @Override public int getItemCount() { return urls.isEmpty() ? 1 : urls.size(); }
    }
}

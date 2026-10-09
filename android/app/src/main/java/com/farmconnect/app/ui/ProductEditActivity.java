package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.farmconnect.app.data.ApiClient;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.data.Models.Product;
import com.farmconnect.app.data.Models.ProductRequest;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.databinding.ActivityProductEditBinding;
import com.farmconnect.app.util.ImageUtil;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Add / edit a product: up to 6 photos, category, price, stock, short summary and full description. */
public class ProductEditActivity extends AppCompatActivity {
    private static final int MAX_PHOTOS = 6;
    static final List<String> CATEGORIES = Arrays.asList("Vegetables", "Fruits", "Grains & cereals", "Legumes",
            "Dairy", "Meat", "Poultry & eggs", "Fish", "Herbs & spices", "Other");
    private static final List<String> UNITS = Arrays.asList("kg", "g", "litre", "piece", "bunch", "crate", "bag",
            "tray", "dozen", "sack");

    private ActivityProductEditBinding b;
    private FarmerViewModel vm;
    private List<Farm> farms = new ArrayList<>();
    private Farm selectedFarm;
    private final List<String> existing = new ArrayList<>();   // photos already on the server
    private final List<Uri> pending = new ArrayList<>();       // newly picked, uploaded on save
    private long productId = -1;
    private ActivityResultLauncher<PickVisualMediaRequest> picker;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        productId = getIntent().getLongExtra("id", -1);
        setTitle(productId > 0 ? I18n.t("Edit product") : I18n.t("New product"));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b = ActivityProductEditBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);

        picker = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS), uris -> {
            for (Uri u : uris) {
                if (existing.size() + pending.size() >= MAX_PHOTOS) { Ui.toast(this, I18n.t("Maximum ") + MAX_PHOTOS + I18n.t(" photos")); break; }
                pending.add(u);
            }
            renderPhotos();
        });

        b.etCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES));
        b.etUnit.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, UNITS));
        b.etUnit.setText(I18n.t("kg"), false);
        b.btnSave.setOnClickListener(v -> save());
        renderPhotos();

        Ui.watch(this, vm.myFarms(), b.progress, fs -> {
            farms = fs;
            if (fs.isEmpty()) { Ui.toast(this, I18n.t("Create a farm first (My farms)")); finish(); return; }
            b.etFarm.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, farms));
            b.etFarm.setOnItemClickListener((p, v, pos, id) -> { selectedFarm = farms.get(pos); showNote(); });
            selectedFarm = farms.get(0);
            b.etFarm.setText(selectedFarm.name, false);
            showNote();
            if (productId > 0) Ui.watch(this, vm.product(productId), b.progress, this::fill);
        });
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void showNote() {
        b.tvVisibilityNote.setVisibility(View.VISIBLE);
        if (selectedFarm != null && selectedFarm.ownerVerified) {
            b.tvVisibilityNote.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F1E0));
            b.tvVisibilityNote.setTextColor(0xFF1B5E20);
            b.tvVisibilityNote.setText(I18n.t("✓ You are a verified farmer. This product goes live in the marketplace as soon as you save."));
        } else {
            b.tvVisibilityNote.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF4D6));
            b.tvVisibilityNote.setTextColor(0xFF7A5600);
            b.tvVisibilityNote.setText(I18n.t("⏳ You can add products now, but buyers will only see them after your farmer verification is approved."));
        }
    }

    private static String num(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }

    private void fill(Product p) {
        b.etName.setText(p.name);
        b.etCategory.setText(p.category, false);
        b.etPrice.setText(num(p.price));
        b.etQty.setText(String.valueOf(p.quantity));
        if (p.unit != null && !p.unit.isEmpty()) b.etUnit.setText(p.unit, false);
        b.etSummary.setText(p.summary);
        b.etDescription.setText(p.description);
        for (Farm f : farms) if (f.id == p.farmId) { selectedFarm = f; b.etFarm.setText(f.name, false); }
        showNote();
        existing.clear();
        if (p.imageUrls != null) existing.addAll(p.imageUrls);
        renderPhotos();
    }

    // ---------------- photos ----------------

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void renderPhotos() {
        b.photoRow.removeAllViews();
        for (int i = 0; i < existing.size(); i++) {
            final int index = i;
            b.photoRow.addView(thumb(ApiClient.absolute(existing.get(i)), i == 0, () -> confirmRemoveExisting(index)));
        }
        for (int i = 0; i < pending.size(); i++) {
            final int index = i;
            b.photoRow.addView(thumb(pending.get(i), existing.isEmpty() && i == 0, () -> { pending.remove(index); renderPhotos(); }));
        }
        if (existing.size() + pending.size() < MAX_PHOTOS) b.photoRow.addView(addTile());
        b.tvPhotoHint.setText(existing.size() + pending.size() + I18n.t(" of ") + MAX_PHOTOS
                + I18n.t(" photos. The first one is the cover buyers see in the list."));
    }

    private View addTile() {
        TextView t = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(104), dp(104));
        t.setLayoutParams(lp);
        t.setGravity(Gravity.CENTER);
        t.setText(I18n.t("＋\nAdd photos"));
        t.setTextColor(0xFF2E7D32);
        t.setTextSize(13);
        t.setBackgroundResource(com.farmconnect.app.R.drawable.bg_add_tile);
        t.setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        return t;
    }

    private View thumb(Object model, boolean cover, Runnable onRemove) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(104), dp(104));
        lp.setMarginEnd(dp(10));
        card.setLayoutParams(lp);
        card.setRadius(dp(14));
        card.setCardElevation(0);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(iv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        Glide.with(this).load(model).centerCrop().placeholder(com.farmconnect.app.R.drawable.ph_product).into(iv);

        if (cover) {
            TextView c = new TextView(this);
            c.setText(I18n.t("Cover"));
            c.setTextColor(0xFFFFFFFF);
            c.setTextSize(11);
            c.setPadding(dp(8), dp(2), dp(8), dp(2));
            c.setBackgroundColor(0xB3000000);
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            cp.gravity = Gravity.BOTTOM | Gravity.START;
            card.addView(c, cp);
        }
        TextView x = new TextView(this);
        x.setText("✕");
        x.setGravity(Gravity.CENTER);
        x.setTextColor(0xFFFFFFFF);
        x.setTextSize(12);
        x.setBackgroundResource(com.farmconnect.app.R.drawable.bg_icon_dark);
        FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(dp(26), dp(26));
        xp.gravity = Gravity.TOP | Gravity.END;
        xp.setMargins(0, dp(6), dp(6), 0);
        x.setOnClickListener(v -> onRemove.run());
        card.addView(x, xp);
        return card;
    }

    private void confirmRemoveExisting(int index) {
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("Remove this photo?"))
                .setPositiveButton(I18n.t("Remove"), (d, w) -> Ui.watch(this, vm.deleteProductImage(productId, index), b.progress, p -> {
                    existing.clear();
                    if (p.imageUrls != null) existing.addAll(p.imageUrls);
                    renderPhotos();
                }))
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    // ---------------- save ----------------

    private static String text(android.widget.TextView t) { return t.getText() == null ? "" : t.getText().toString().trim(); }

    private void save() {
        b.tilName.setError(null); b.tilPrice.setError(null); b.tilQty.setError(null);
        String name = text(b.etName);
        if (name.isEmpty()) { b.tilName.setError(I18n.t("Enter the product name")); b.etName.requestFocus(); return; }
        double price; int qty;
        try { price = Double.parseDouble(text(b.etPrice)); if (price < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { b.tilPrice.setError(I18n.t("Enter a valid price")); return; }
        try { qty = Integer.parseInt(text(b.etQty)); if (qty < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { b.tilQty.setError(I18n.t("Enter the stock quantity")); return; }
        if (selectedFarm == null) { Ui.toast(this, I18n.t("Choose a farm")); return; }

        ProductRequest r = new ProductRequest(name, text(b.etCategory), price, qty, text(b.etUnit), selectedFarm.id);
        r.summary = text(b.etSummary);
        r.description = text(b.etDescription);

        b.btnSave.setEnabled(false);
        (productId > 0 ? vm.updateProduct(productId, r) : vm.createProduct(r)).observe(this, res -> {
            if (res.status == Resource.Status.LOADING) { b.progress.setVisibility(View.VISIBLE); return; }
            if (res.status == Resource.Status.ERROR) { fail(res.message); return; }
            productId = res.data.id;      // if a photo upload fails, "Save" again just updates this product
            uploadNext();
        });
    }

    private void fail(String msg) {
        b.progress.setVisibility(View.INVISIBLE);
        b.btnSave.setEnabled(true);
        Ui.toast(this, msg);
    }

    /** Uploads the picked photos one by one, then closes the screen. */
    private void uploadNext() {
        if (pending.isEmpty()) {
            b.progress.setVisibility(View.INVISIBLE);
            Ui.toast(this, selectedFarm != null && selectedFarm.ownerVerified
                    ? I18n.t("Product saved") : I18n.t("Saved. Buyers will see it once you are verified."));
            setResult(RESULT_OK);
            finish();
            return;
        }
        final Uri uri = pending.get(0);
        new Thread(() -> {
            try {
                byte[] jpeg = ImageUtil.compress(this, uri);
                runOnUiThread(() -> vm.addProductImage(productId, jpeg).observe(this, res -> {
                    if (res.status == Resource.Status.LOADING) return;
                    if (res.status == Resource.Status.ERROR) { renderPhotos(); fail(I18n.t("Photo upload failed: ") + res.message); return; }
                    pending.remove(0);
                    existing.clear();
                    if (res.data.imageUrls != null) existing.addAll(res.data.imageUrls);
                    uploadNext();
                }));
            } catch (Exception e) {
                runOnUiThread(() -> fail(I18n.t("Could not read one of the photos")));
            }
        }).start();
    }
}

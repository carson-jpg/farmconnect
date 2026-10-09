package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.CartItem;
import com.farmconnect.app.data.Models.OrderRequest;
import com.farmconnect.app.databinding.ActivityCheckoutBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import com.google.gson.Gson;
import java.util.List;

/** Delivery details + payment method + order summary, then places the order. */
public class CheckoutActivity extends AppCompatActivity {
    private ActivityCheckoutBinding b;
    private BuyerViewModel vm;
    private double total;
    private boolean hasItems;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);
        b.btnBack.setOnClickListener(v -> finish());
        b.btnPlace.setEnabled(false);
        b.btnPlace.setOnClickListener(v -> place());
        Ui.watch(this, vm.cart(), b.progress, this::showSummary);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void showSummary(List<CartItem> items) {
        b.summaryContainer.removeAllViews();
        total = 0;
        for (CartItem c : items) {
            total += c.product.price * c.quantity;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));
            String cover = c.product.imageUrls != null && !c.product.imageUrls.isEmpty() ? c.product.imageUrls.get(0) : c.product.imageUrl;
            row.addView(Thumbs.make(this, cover, 48, 12));
            TextView name = new TextView(this);
            name.setText(c.quantity + " × " + c.product.name);
            name.setTextColor(0xFF1C2B1E);
            name.setTextSize(14);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView line = new TextView(this);
            line.setText(Ui.kes(c.product.price * c.quantity));
            line.setTextColor(0xFF1C2B1E);
            line.setTextSize(14);
            line.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            row.addView(line);
            b.summaryContainer.addView(row);
        }
        hasItems = !items.isEmpty();
        b.tvTotal.setText(Ui.kes(total));
        b.btnPlace.setText(I18n.t("Place order · ") + Ui.kes(total));
        b.btnPlace.setEnabled(hasItems);
        b.tvSubtitle.setText(hasItems ? items.size() + (items.size() == 1 ? I18n.t(" product") : I18n.t(" products")) : I18n.t("Your cart is empty"));
    }

    private void place() {
        b.tilPhone.setError(null);
        b.tilAddress.setError(null);
        String phone = b.etPhone.getText().toString().replaceAll("[\\s-]", "");
        String address = b.etAddress.getText().toString().trim();
        String note = b.etNote.getText().toString().trim();
        if (!phone.matches("\\+?\\d{9,13}")) { b.tilPhone.setError(I18n.t("Enter a valid phone number")); b.etPhone.requestFocus(); return; }
        if (address.length() < 5) { b.tilAddress.setError(I18n.t("Enter where we should deliver")); b.etAddress.requestFocus(); return; }
        String pay = b.rbMpesa.isChecked() ? "MPESA_ON_DELIVERY" : "CASH_ON_DELIVERY";

        b.btnPlace.setEnabled(false);
        vm.placeOrder(new OrderRequest(address, phone, note, pay)).observe(this, res -> {
            switch (res.status) {
                case LOADING:
                    b.progress.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    b.progress.setVisibility(View.INVISIBLE);
                    b.btnPlace.setEnabled(hasItems);
                    Ui.toast(this, res.message);
                    break;
                default:
                    b.progress.setVisibility(View.INVISIBLE);
                    startActivity(new Intent(this, OrderSuccessActivity.class).putExtra("order", new Gson().toJson(res.data)));
                    finish();
            }
        });
    }
}

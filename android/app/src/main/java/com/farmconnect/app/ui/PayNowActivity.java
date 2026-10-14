package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.databinding.ActivitySimplePageBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

/**
 * Pay for an order inside the app: sends the M-Pesa prompt to the buyer's phone, then checks every few seconds
 * until the server reports PAID or FAILED. The amount is never sent from here: the server uses the order total.
 */
public class PayNowActivity extends AppCompatActivity {
    private static final int MAX_POLLS = 30;          // 30 x 4 s = 2 minutes of waiting
    private static final long POLL_MS = 4000;

    private ActivitySimplePageBinding b;
    private BuyerViewModel vm;
    private Order order;
    private String phone;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = this::poll;
    private int polls;
    private boolean finished;
    private TextView tvHead, tvBody;
    private LinearLayout buttons;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySimplePageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(BuyerViewModel.class);
        order = new Gson().fromJson(getIntent().getStringExtra("order"), Order.class);
        phone = getIntent().getStringExtra("phone");
        if (phone == null) phone = order.deliveryPhone != null ? order.deliveryPhone : order.buyerPhone;

        b.tvTitle.setText(I18n.t("Pay with M-Pesa"));
        b.tvSubtitle.setText(I18n.t("Order #") + order.id + " · " + Ui.kes(order.total));
        b.btnBack.setOnClickListener(v -> leave());

        MaterialCardView card = Cards.card(this, b.content, 12);
        LinearLayout in = Cards.inner(card);
        tvHead = Cards.text(this, "", 20, 0xFF1C2B1E, true);
        Cards.center(tvHead);
        tvBody = Cards.text(this, "", 15, 0xFF4A5A4C, false);
        Cards.center(tvBody);
        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        in.addView(tvHead);
        in.addView(Cards.spacer(this, 8));
        in.addView(tvBody);
        in.addView(Cards.spacer(this, 16));
        in.addView(buttons);
        start();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void leave() {
        if (!finished) Ui.toast(this, "Your order is saved. You can pay from My orders.");
        finish();
    }

    /** Sends a fresh M-Pesa prompt. */
    private void start() {
        finished = false;
        polls = 0;
        handler.removeCallbacksAndMessages(null);
        buttons.removeAllViews();
        show("📲  " + I18n.t("Sending the payment request…"), I18n.t("Please wait a moment."), true);
        vm.startStk(order.id, phone).observe(this, r -> {
            if (r.status == Resource.Status.LOADING) return;
            if (r.status == Resource.Status.ERROR) { fail(r.message); return; }
            waiting();
        });
    }

    private void waiting() {
        buttons.removeAllViews();
        show("📱  " + I18n.t("Check your phone"),
                I18n.t("An M-Pesa request for ") + Ui.kes(order.total) + I18n.t(" was sent to ") + phone
                        + I18n.t(". Enter your M-Pesa PIN to pay. This page updates by itself."), true);
        addButton(I18n.t("I will pay later"), false, this::leave);
        handler.postDelayed(pollTask, POLL_MS);
    }

    private void poll() {
        if (finished) return;
        polls++;
        vm.payStatus(order.id).observe(this, r -> {
            if (finished || r.status == Resource.Status.LOADING) return;
            if (r.status == Resource.Status.SUCCESS && r.data != null) {
                if ("PAID".equals(r.data.paymentStatus)) { paid(r.data.receipt); return; }
                if ("FAILED".equals(r.data.paymentStatus)) {
                    fail(I18n.t("The payment was not completed. If money was taken from you it will be returned."));
                    return;
                }
            }
            if (polls >= MAX_POLLS) timeout(); else handler.postDelayed(pollTask, POLL_MS);   // a network blip just waits for the next check
        });
    }

    private void paid(String receipt) {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        order.paymentStatus = "PAID";
        order.mpesaReceipt = receipt;
        startActivity(new Intent(this, OrderSuccessActivity.class).putExtra("order", new Gson().toJson(order)));
        finish();
    }

    private void fail(String message) {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        buttons.removeAllViews();
        show("⚠️  " + I18n.t("Payment not completed"), message == null ? "" : message, false);
        addButton(I18n.t("Try again"), true, this::askPhone);
        addButton(I18n.t("View my order"), false, this::openOrder);
    }

    private void timeout() {
        finished = true;
        buttons.removeAllViews();
        show("⏳  " + I18n.t("Still waiting"),
                I18n.t("If you entered your PIN, the payment will show on your order shortly. You can check again."), false);
        addButton(I18n.t("Check again"), true, () -> { finished = false; polls = 0; waiting(); });
        addButton(I18n.t("Send a new request"), false, this::askPhone);
        addButton(I18n.t("View my order"), false, this::openOrder);
    }

    private void openOrder() {
        startActivity(new Intent(this, OrderDetailActivity.class).putExtra("order", new Gson().toJson(order)).putExtra("mode", "BUYER"));
        finish();
    }

    /** Lets the buyer change the M-Pesa number before sending a new request. */
    private void askPhone() {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_PHONE);
        et.setText(phone);
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("M-Pesa number"))
                .setMessage(I18n.t("Send the payment request to this number:"))
                .setView(et)
                .setPositiveButton(I18n.t("Send request"), (d, w) -> {
                    String p = et.getText().toString().replaceAll("[\\s-]", "");
                    if (!p.matches("\\+?\\d{9,13}")) { Ui.toast(this, "Enter a valid phone number"); return; }
                    phone = p;
                    start();
                })
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    private void show(String head, String body, boolean busy) {
        tvHead.setText(head);
        tvBody.setText(body);
        b.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    private void addButton(String text, boolean primary, Runnable action) {
        MaterialButton btn = primary
                ? new MaterialButton(this)
                : new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(8 * getResources().getDisplayMetrics().density);
        buttons.addView(btn, lp);
    }
}

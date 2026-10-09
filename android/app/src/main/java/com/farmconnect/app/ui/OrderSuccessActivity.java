package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.databinding.ActivityOrderSuccessBinding;
import com.farmconnect.app.util.Ui;
import com.google.gson.Gson;

public class OrderSuccessActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityOrderSuccessBinding b = ActivityOrderSuccessBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        final String json = getIntent().getStringExtra("order");
        Order o = new Gson().fromJson(json, Order.class);

        int n = 0;
        if (o.items != null) for (com.farmconnect.app.data.Models.OrderItem i : o.items) n += i.quantity;
        b.tvOrderNo.setText(I18n.t("Order #") + o.id);
        b.tvItemsLine.setText(n + (n == 1 ? I18n.t(" item") : I18n.t(" items")) + I18n.t(" ordered"));
        b.tvTotalLine.setText(Ui.kes(o.total));
        b.tvPayLine.setText("💳  " + Ui.paymentLabel(o.paymentMethod));
        b.tvAddrLine.setText("📍  " + o.deliveryAddress);

        b.btnTrack.setOnClickListener(v -> {
            startActivity(new Intent(this, OrderDetailActivity.class).putExtra("order", json).putExtra("mode", "BUYER"));
            finish();
        });
        b.btnContinue.setOnClickListener(v -> Ui.home(this));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { Ui.home(OrderSuccessActivity.this); }
        });
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Models.OrderItem;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.databinding.ActivityOrderDetailBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AdminViewModel;
import com.farmconnect.app.vm.BuyerViewModel;
import com.farmconnect.app.vm.FarmerViewModel;
import com.google.gson.Gson;

/** Full order page: progress timeline, items with photos, delivery, payment and the actions for the viewer's role. */
public class OrderDetailActivity extends AppCompatActivity {
    private static final String[] TITLES = {"Order placed", "Confirmed by seller", "On the way", "Delivered"};
    private static final String[] HINTS = {"We sent your order to the seller.", "The seller is preparing your produce.",
            "Your order is being delivered.", "Enjoy your fresh produce!"};

    private ActivityOrderDetailBinding b;
    private OrderAdapter.Mode mode;
    private Order order;
    private BuyerViewModel buyerVm;
    private FarmerViewModel farmerVm;
    private AdminViewModel adminVm;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        buyerVm = new ViewModelProvider(this).get(BuyerViewModel.class);
        farmerVm = new ViewModelProvider(this).get(FarmerViewModel.class);
        adminVm = new ViewModelProvider(this).get(AdminViewModel.class);
        mode = OrderAdapter.Mode.valueOf(getIntent().getStringExtra("mode") == null ? "BUYER" : getIntent().getStringExtra("mode"));
        order = new Gson().fromJson(getIntent().getStringExtra("order"), Order.class);
        b.btnBack.setOnClickListener(v -> finish());
        render();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void render() {
        Order o = order;
        b.tvTitle.setText(I18n.t("Order #") + o.id);
        b.tvSubtitle.setText(I18n.t("Placed ") + Ui.dateTime(o.createdAt));
        Ui.styleStatus(b.tvStatus, o.status);
        b.tvCancelled.setVisibility("CANCELLED".equals(o.status) ? View.VISIBLE : View.GONE);

        // ---- timeline
        b.timeline.removeAllViews();
        int reached = OrderAdapter.step(o.status);
        if (reached >= 0) {
            for (int i = 0; i < 4; i++) b.timeline.addView(timelineRow(i, reached, o));
        } else {
            b.timeline.addView(simpleLine(I18n.t("Order placed"), Ui.dateTime(o.createdAt)));
            b.timeline.addView(simpleLine(I18n.t("Cancelled"), Ui.dateTime(o.updatedAt)));
        }

        // ---- items
        b.itemsContainer.removeAllViews();
        for (OrderItem i : o.items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.addView(Thumbs.make(this, i.imageUrl, 56, 12));
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView name = new TextView(this);
            name.setText(i.productName);
            name.setTextColor(0xFF1C2B1E);
            name.setTextSize(15);
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            TextView sub = new TextView(this);
            String unit = i.unit == null || i.unit.isEmpty() ? "" : " / " + i.unit;
            sub.setText(i.quantity + " × " + Ui.kes(i.price) + unit + (i.farmName == null ? "" : "  ·  " + i.farmName));
            sub.setTextColor(0xFF5E7061);
            sub.setTextSize(12);
            col.addView(name);
            col.addView(sub);
            row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView line = new TextView(this);
            line.setText(Ui.kes(i.price * i.quantity));
            line.setTextColor(0xFF1C2B1E);
            line.setTextSize(14);
            line.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            row.addView(line);
            b.itemsContainer.addView(row);
        }
        b.tvTotal.setText(Ui.kes(o.total));

        // ---- delivery & payment
        if (mode == OrderAdapter.Mode.BUYER) {
            b.tvBuyer.setVisibility(View.GONE);
        } else {
            b.tvBuyer.setVisibility(View.VISIBLE);
            b.tvBuyer.setText("👤 " + (o.buyerName == null ? I18n.t("Customer") : o.buyerName));
        }
        b.tvAddress.setText("📍 " + o.deliveryAddress);
        String phone = o.deliveryPhone != null && !o.deliveryPhone.isEmpty() ? o.deliveryPhone : o.buyerPhone;
        b.tvPhone.setVisibility(phone == null || phone.isEmpty() ? View.GONE : View.VISIBLE);
        b.tvPhone.setText("📞 " + phone);
        boolean hasNote = o.deliveryNote != null && !o.deliveryNote.trim().isEmpty();
        b.tvNote.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        if (hasNote) b.tvNote.setText("“" + o.deliveryNote + "”");
        long otherId = mode == OrderAdapter.Mode.BUYER ? (o.items.isEmpty() ? -1 : o.items.get(0).farmerId) : o.buyerId;
        String otherName = mode == OrderAdapter.Mode.BUYER ? (o.items.isEmpty() || o.items.get(0).farmName == null ? I18n.t("Seller") : o.items.get(0).farmName)
                : (o.buyerName == null ? I18n.t("Customer") : o.buyerName);
        b.btnMessage.setVisibility(otherId > 0 && mode != OrderAdapter.Mode.ADMIN ? View.VISIBLE : View.GONE);
        b.btnMessage.setText(mode == OrderAdapter.Mode.BUYER ? I18n.t("💬 Message the seller") : I18n.t("💬 Message the buyer"));
        b.btnMessage.setOnClickListener(v -> startActivity(new android.content.Intent(this, ChatActivity.class)
                .putExtra("userId", otherId).putExtra("name", otherName)));
        boolean canRate = mode == OrderAdapter.Mode.BUYER && "DELIVERED".equals(o.status) && !o.items.isEmpty();
        b.btnRate.setVisibility(canRate ? View.VISIBLE : View.GONE);
        b.btnRate.setOnClickListener(v -> rateSellers(o));
        b.tvPayment.setText("💳 " + Ui.paymentLabel(o.paymentMethod));

        // ---- actions by role
        boolean showAction, showSecondary = false;
        String text;
        if (mode == OrderAdapter.Mode.BUYER) {
            showAction = "PENDING".equals(o.status);
            text = I18n.t("Cancel order");
        } else {
            text = OrderAdapter.actionLabel(o.status);
            showAction = text != null;
            showSecondary = mode == OrderAdapter.Mode.ADMIN && reached >= 0 && reached < 3;
        }
        b.actionBar.setVisibility(showAction || showSecondary ? View.VISIBLE : View.GONE);
        b.btnAction.setVisibility(showAction ? View.VISIBLE : View.GONE);
        if (showAction) b.btnAction.setText(text);
        b.btnSecondary.setVisibility(showSecondary ? View.VISIBLE : View.GONE);
        b.btnAction.setOnClickListener(v -> {
            if (mode == OrderAdapter.Mode.BUYER) confirmCancel(this::cancelAsBuyer);
            else advance();
        });
        b.btnSecondary.setOnClickListener(v -> confirmCancel(() -> run(adminVm.setOrderStatus(order.id, "CANCELLED"))));
    }

    private void advance() {
        String next = OrderAdapter.next(order.status);
        if (mode == OrderAdapter.Mode.ADMIN) run(adminVm.setOrderStatus(order.id, next));
        else run(farmerVm.updateStatus(order.id, next));
    }

    private void cancelAsBuyer() { run(buyerVm.cancelOrder(order.id)); }

    private void run(LiveData<Resource<Order>> call) {
        Ui.watch(this, call, b.progress, updated -> {
            order = updated;
            setResult(RESULT_OK);
            render();
        });
    }

    private void confirmCancel(Runnable r) {
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("Cancel this order?"))
                .setMessage(I18n.t("The reserved stock goes back to the seller. This cannot be undone."))
                .setPositiveButton(I18n.t("Cancel order"), (d, w) -> r.run())
                .setNegativeButton(I18n.t("Keep order"), null)
                .show();
    }

    private View simpleLine(String title, String when) {
        TextView t = new TextView(this);
        t.setText(title + (when == null || when.isEmpty() ? "" : "  ·  " + when));
        t.setTextColor(0xFF1C2B1E);
        t.setTextSize(14);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    /** Dot + connecting line on the left, title/hint on the right. */
    private View timelineRow(int i, int reached, Order o) {
        boolean done = i <= reached;
        boolean current = i == reached;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView dot = new TextView(this);
        dot.setGravity(Gravity.CENTER);
        dot.setText(done ? "✓" : String.valueOf(i + 1));
        dot.setTextColor(done ? 0xFFFFFFFF : 0xFF5E7061);
        dot.setTextSize(12);
        dot.setBackgroundResource(done ? com.farmconnect.app.R.drawable.bg_dot_done : com.farmconnect.app.R.drawable.bg_dot_todo);
        rail.addView(dot, new LinearLayout.LayoutParams(dp(28), dp(28)));
        if (i < 3) {
            View line = new View(this);
            line.setBackgroundColor(i < reached ? 0xFF2E7D32 : 0xFFD5E3D0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(3), dp(30));
            rail.addView(line, lp);
        }
        row.addView(rail, new LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(14), 0, 0, dp(10));
        TextView title = new TextView(this);
        title.setText(TITLES[i]);
        title.setTextSize(15);
        title.setTextColor(done ? 0xFF1C2B1E : 0xFF8A9A8C);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView hint = new TextView(this);
        String when = i == 0 ? Ui.dateTime(o.createdAt) : (current ? Ui.dateTime(o.updatedAt) : "");
        hint.setText(done && !when.isEmpty() ? when : HINTS[i]);
        hint.setTextSize(12);
        hint.setTextColor(0xFF5E7061);
        text.addView(title);
        text.addView(hint);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    /** One review per seller in the order. If several sellers are in the order the buyer picks one. */
    private void rateSellers(Order o) {
        java.util.LinkedHashMap<Long, String> sellers = new java.util.LinkedHashMap<>();
        for (OrderItem i : o.items) if (i.farmerId > 0) sellers.putIfAbsent(i.farmerId, i.farmName == null ? I18n.t("Seller") : i.farmName);
        if (sellers.isEmpty()) return;
        if (sellers.size() == 1) {
            java.util.Map.Entry<Long, String> e = sellers.entrySet().iterator().next();
            rateDialog(o.id, e.getKey(), e.getValue());
            return;
        }
        final Long[] ids = sellers.keySet().toArray(new Long[0]);
        final String[] names = sellers.values().toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle(I18n.t("Rate which seller?"))
                .setItems(names, (d, w) -> rateDialog(o.id, ids[w], names[w])).show();
    }

    private void rateDialog(long orderId, long sellerId, String farmName) {
        java.util.List<Forms.Field> f = new java.util.ArrayList<>();
        f.add(Forms.choice("rating", I18n.t("Your rating"), true, Forms.arr("5", "4", "3", "2", "1"),
                Forms.arr("★★★★★  Excellent", "★★★★☆  Good", "★★★☆☆  Okay", "★★☆☆☆  Poor", "★☆☆☆☆  Bad")).value("5"));
        f.add(Forms.multi("comment", I18n.t("Tell other buyers about the produce and delivery (optional)"), false));
        Forms.show(this, I18n.t("Rate ") + farmName, I18n.t("Submit"), f, v ->
                Ui.watch(this, new ViewModelProvider(this).get(com.farmconnect.app.vm.CommunityViewModel.class)
                        .createReview(orderId, sellerId, Integer.parseInt(v.get("rating")), v.get("comment")), b.progress,
                        r -> Ui.toast(this, I18n.t("Thank you! Your review helps other buyers."))));
    }
}

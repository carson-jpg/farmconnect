package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Order;
import com.farmconnect.app.data.Models.OrderItem;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.BuyerViewModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A buyer's spending at a glance: numbers, spend per month, orders by status, most-bought products. */
public class BuyerInsightsActivity extends BaseListActivity {
    private BuyerViewModel bvm;

    @Override protected String screenTitle() { return I18n.t("My spending"); }
    @Override protected String emptyText() { return I18n.t("You have not placed any orders yet"); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        bvm = new ViewModelProvider(this).get(BuyerViewModel.class);
    }

    @Override protected void load() {
        if (bvm == null) return;
        Ui.watch(this, bvm.orders(), b.progress, orders -> {
            LinearLayout top = b.topArea;
            top.removeAllViews();
            top.setPadding(Cards.dp(this, 12), Cards.dp(this, 6), Cards.dp(this, 12), 0);
            double spent = 0;
            int delivered = 0, active = 0;
            LinkedHashMap<String, Double> months = Insights.months(6);
            LinkedHashMap<String, Long> status = new LinkedHashMap<>();
            Map<String, Long> units = new LinkedHashMap<>();
            for (Order o : orders) {
                String label = Insights.title(o.status);
                status.put(label, (status.containsKey(label) ? status.get(label) : 0L) + 1);
                if ("CANCELLED".equals(o.status)) continue;
                spent += o.total;
                if ("DELIVERED".equals(o.status)) delivered++; else active++;
                Insights.add(months, o.createdAt, o.total);
                if (o.items != null) for (OrderItem i : o.items) units.put(i.productName, (units.containsKey(i.productName) ? units.get(i.productName) : 0L) + i.quantity);
            }
            List<android.view.View> t = new ArrayList<>();
            t.add(UiKit.statCard(this, "💸", I18n.t("Total spent"), Ui.kes(spent), I18n.t("excluding cancelled"), 0xFF2E7D32, null));
            t.add(UiKit.statCard(this, "🧾", I18n.t("Orders"), String.valueOf(orders.size()), null, 0xFF1E88E5, null));
            t.add(UiKit.statCard(this, "🚚", I18n.t("On the way"), String.valueOf(active), I18n.t("not delivered yet"), active > 0 ? 0xFFF9A825 : 0xFF2E7D32, null));
            t.add(UiKit.statCard(this, "✅", I18n.t("Delivered"), String.valueOf(delivered), null, 0xFF2E7D32, null));
            UiKit.grid(this, top, t);
            LinearLayout c1 = UiKit.chartCard(this, top, I18n.t("Spend per month (KSh)"));
            c1.addView(Charts.columns(this, months, 0xFF2E7D32));
            Charts.donut(this, UiKit.chartCard(this, top, I18n.t("Orders by status")), status, I18n.t("orders"));
            List<Map.Entry<String, Long>> es = new ArrayList<>(units.entrySet());
            java.util.Collections.sort(es, (x, y) -> Long.compare(y.getValue(), x.getValue()));
            LinkedHashMap<String, Long> topUnits = new LinkedHashMap<>();
            for (int k = 0; k < Math.min(5, es.size()); k++) topUnits.put(es.get(k).getKey(), es.get(k).getValue());
            Cards.bars(this, UiKit.chartCard(this, top, I18n.t("Most bought")), topUnits, 0xFF2E7D32, null);

            List<Row> rows = new ArrayList<>();
            for (int k = 0; k < Math.min(5, orders.size()); k++) {
                Order o = orders.get(k);
                rows.add(Row.of("🧾", I18n.t("Order #") + o.id).sub(Ui.kes(o.total) + " · " + Insights.title(o.status)).meta(Ui.dateTime(o.createdAt))
                        .click(() -> startActivity(new Intent(this, OrdersActivity.class))));
            }
            show(rows, I18n.t("Latest orders"));
        });
    }
}

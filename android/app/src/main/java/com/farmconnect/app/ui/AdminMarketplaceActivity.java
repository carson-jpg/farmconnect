package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.BulkOrder;
import com.farmconnect.app.data.Models.BulkRequest;
import com.farmconnect.app.data.Models.HireRequest;
import com.farmconnect.app.data.Models.MarketOverview;
import com.farmconnect.app.data.Models.SupplyOrder;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.MarketViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Super admin: read-only overview of everything companies do on the platform. */
public class AdminMarketplaceActivity extends BaseListActivity {
    private MarketViewModel mvm;

    @Override protected String screenTitle() { return I18n.t("Marketplace"); }
    @Override protected String emptyText() { return I18n.t("Nothing here yet"); }

    @Override protected String[][] chips() {
        return new String[][]{{"supply", "Input orders"}, {"requests", "Produce requests"}, {"orders", "Purchase orders"}, {"services", "Service requests"}};
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        mvm = new ViewModelProvider(this).get(MarketViewModel.class);
        Ui.watch(this, mvm.overview(), b.progress, this::kpis);
    }

    private static long n(Map<String, Long> m, String k) { Long v = m == null ? null : m.get(k); return v == null ? 0 : v; }

    private void kpis(MarketOverview o) {
        LinearLayout top = b.topArea;
        top.removeAllViews();
        top.setPadding(Cards.dp(this, 12), Cards.dp(this, 6), Cards.dp(this, 12), 0);
        LinearLayout r1 = Cards.statRow(this, top);
        r1.addView(Cards.stat(this, String.valueOf(n(o.companies, "VERIFIED")), I18n.t("Verified companies")));
        r1.addView(Cards.stat(this, String.valueOf(n(o.companies, "PENDING_VERIFICATION") + n(o.companies, "UNDER_REVIEW")), I18n.t("Awaiting review")));
        r1.addView(Cards.stat(this, Ui.kes(o.supplyDeliveredGross), I18n.t("Input sales")));
        LinearLayout r2 = Cards.statRow(this, top);
        r2.addView(Cards.stat(this, String.valueOf(n(o.supplyOrders, "PLACED") + n(o.supplyOrders, "CONFIRMED") + n(o.supplyOrders, "DISPATCHED")), I18n.t("Open orders")));
        r2.addView(Cards.stat(this, String.valueOf(n(o.produceRequests, "OPEN")), I18n.t("Open requests")));
        r2.addView(Cards.stat(this, String.valueOf(n(o.serviceRequests, "REQUESTED") + n(o.serviceRequests, "ACCEPTED")), I18n.t("Service jobs")));
    }

    @Override protected void load() {
        if (mvm == null) return;
        switch (chip) {
            case "requests":
                Ui.watch(this, mvm.adminProduceRequests(), b.progress, list -> {
                    List<Row> rows = new ArrayList<>();
                    for (BulkRequest r : list)
                        rows.add(Row.of("🌽", r.companyName + " · " + r.produce).sub(CompanyLabels.qty(r.quantity) + " " + r.unit + " · " + r.deliveryCounty)
                                .meta(I18n.t("Closes ") + r.closesAt + " · " + r.openQuotations + I18n.t(" quotes"))
                                .badge(CompanyLabels.name(CompanyLabels.REQ_STATUS_KEYS, CompanyLabels.REQ_STATUS_NAMES, r.status), "OPEN".equals(r.status) ? 0xFF2E7D32 : 0xFF78909C));
                    show(rows, list.size() + I18n.t(" latest"));
                });
                break;
            case "orders":
                Ui.watch(this, mvm.adminPurchaseOrders(), b.progress, list -> {
                    List<Row> rows = new ArrayList<>();
                    for (BulkOrder o : list)
                        rows.add(Row.of("📑", "#" + o.id + " · " + o.companyName + " → " + o.farmerName).sub(o.produce + " · " + CompanyLabels.qty(o.quantity) + " " + o.unit)
                                .meta(CompanyLabels.money(o.total) + ("PAID".equals(o.paymentStatus) ? " · " + I18n.t("Paid") : ""))
                                .badge(CompanyLabels.name(CompanyLabels.PO_STATUS_KEYS, CompanyLabels.PO_STATUS_NAMES, o.status), CompanyLabels.poColor(o.status)));
                    show(rows, list.size() + I18n.t(" latest"));
                });
                break;
            case "services":
                Ui.watch(this, mvm.adminServiceRequests(), b.progress, list -> {
                    List<Row> rows = new ArrayList<>();
                    for (HireRequest r : list)
                        rows.add(Row.of("📨", r.companyName + " · " + r.serviceName).sub(r.farmerName + " · " + r.location).meta(Ui.dateTime(r.createdAt))
                                .badge(CompanyLabels.name(CompanyLabels.HIRE_STATUS_KEYS, CompanyLabels.HIRE_STATUS_NAMES, r.status), CompanyLabels.hireColor(r.status)));
                    show(rows, list.size() + I18n.t(" latest"));
                });
                break;
            default:
                Ui.watch(this, mvm.adminSupplyOrders(), b.progress, list -> {
                    List<Row> rows = new ArrayList<>();
                    for (SupplyOrder o : list)
                        rows.add(Row.of("🧾", "#" + o.id + " · " + o.companyName + " → " + o.buyerName).sub(CompanyLabels.itemsSummary(o.items))
                                .meta(CompanyLabels.money(o.total) + " · " + Ui.dateTime(o.createdAt))
                                .badge(CompanyLabels.name(CompanyLabels.ORDER_STATUSES, CompanyLabels.ORDER_STATUS_NAMES, o.status), CompanyLabels.orderColor(o.status)));
                    show(rows, list.size() + I18n.t(" latest"));
                });
                break;
        }
    }
}

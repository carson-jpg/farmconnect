package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.PayoutView;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.List;

/** What FarmConnect has sent (or is sending) to the company's payout account. */
public class CompanyPayoutsActivity extends BaseListActivity {
    private SupplyViewModel svm;

    @Override protected String screenTitle() { return I18n.t("Payouts"); }
    @Override protected String emptyText() { return I18n.t("No payouts yet.\nThey appear when a farmer pays an order by M-Pesa."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        svm = new ViewModelProvider(this).get(SupplyViewModel.class);
    }

    private static String statusName(String s) {
        switch (s) {
            case "SUCCESS": return "Paid out";
            case "PROCESSING": return "Sending";
            case "FAILED": return "Failed";
            case "UNKNOWN": return "Being checked";
            default: return "Queued";
        }
    }

    @Override protected void load() {
        Ui.watch(this, svm.payouts(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (PayoutView p : list) {
                int color = "SUCCESS".equals(p.status) ? 0xFF2E7D32 : "FAILED".equals(p.status) ? 0xFFE53935 : 0xFFF9A825;
                rows.add(Row.of("💸", CompanyLabels.money(p.amountKes) + " · " + I18n.t("order #") + p.orderId)
                        .sub("SUCCESS".equals(p.status) ? (p.providerTransactionId == null ? "" : p.providerTransactionId) : (p.failureReason == null ? "" : p.failureReason))
                        .meta(Ui.dateTime(p.completedAt != null ? p.completedAt : p.createdAt))
                        .badge(I18n.t(statusName(p.status)), color));
            }
            show(rows, list.size() + I18n.t(" payouts"));
        });
    }
}

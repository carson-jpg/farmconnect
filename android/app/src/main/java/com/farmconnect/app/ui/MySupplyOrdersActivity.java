package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.SupplyOrder;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.SupplyViewModel;
import java.util.ArrayList;
import java.util.List;

/** A farmer's orders for farm inputs. They can cancel while the company has not yet handled the order. */
public class MySupplyOrdersActivity extends BaseListActivity {
    private SupplyViewModel svm;
    private boolean mpesaOn;

    @Override protected String screenTitle() { return I18n.t("Input orders"); }
    @Override protected String emptyText() { return I18n.t("You have not ordered any farm inputs yet"); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        svm = new ViewModelProvider(this).get(SupplyViewModel.class);
        svm.paymentsEnabled().observe(this, r -> {
            if (r.status == com.farmconnect.app.data.Resource.Status.SUCCESS && r.data != null) mpesaOn = r.data.enabled;
        });
    }

    @Override protected void load() {
        Ui.watch(this, svm.myOrders(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (SupplyOrder o : list) {
                rows.add(Row.of("🧾", "#" + o.id + " · " + o.companyName)
                        .sub(CompanyLabels.itemsSummary(o.items))
                        .meta(CompanyLabels.money(o.total) + " · " + Ui.dateTime(o.createdAt))
                        .badge(CompanyLabels.name(CompanyLabels.ORDER_STATUSES, CompanyLabels.ORDER_STATUS_NAMES, o.status), CompanyLabels.orderColor(o.status))
                        .click(() -> detail(o)));
            }
            show(rows, list.size() + I18n.t(" orders"));
        });
    }

    private void detail(SupplyOrder o) {
        Sheet s = Sheet.of(this, I18n.t("Order #") + o.id)
                .badge(CompanyLabels.name(CompanyLabels.ORDER_STATUSES, CompanyLabels.ORDER_STATUS_NAMES, o.status), CompanyLabels.orderColor(o.status))
                .field(I18n.t("Company"), o.companyName)
                .field(I18n.t("Deliver to"), o.deliveryAddress + (o.deliveryPhone == null ? "" : " · " + o.deliveryPhone))
                .field(I18n.t("Note"), o.deliveryNote)
                .field(I18n.t("Items"), items(o))
                .strong(I18n.t("Total"), CompanyLabels.money(o.total))
                .field(I18n.t("Payment"), payment(o))
                .field(I18n.t("Reason"), o.rejectionReason);
        if (mpesaOn && "DELIVERED".equals(o.status) && "UNPAID".equals(o.paymentStatus)) s.primary(I18n.t("Pay with M-Pesa"), () -> pay(o));
        if ("PLACED".equals(o.status))
            s.danger(I18n.t("Cancel order"), () -> Ui.watch(this, svm.cancel(o.id), b.progress, r -> {
                Ui.toast(this, I18n.t("Order cancelled"));
                load();
            }));
        s.show();
    }

    private static String items(SupplyOrder o) {
        StringBuilder b = new StringBuilder();
        if (o.items != null) for (com.farmconnect.app.data.Models.SupplyItem i : o.items) {
            if (b.length() > 0) b.append('\n');
            b.append(i.quantity).append(" × ").append(i.name).append(" (").append(i.unit).append(")  ").append(CompanyLabels.money(i.lineTotal));
        }
        return b.toString();
    }

    private static String payment(SupplyOrder o) {
        if ("PAID".equals(o.paymentStatus)) return "MPESA".equals(o.paymentMethod) ? I18n.t("Paid by M-Pesa ") + (o.mpesaReceipt == null ? "" : o.mpesaReceipt) : I18n.t("Paid (cash)");
        if ("PENDING".equals(o.paymentStatus)) return I18n.t("M-Pesa payment in progress");
        return I18n.t("Pay the company on delivery");
    }

    private void pay(SupplyOrder o) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.phone("phone", I18n.t("Safaricom number to pay from"), true));
        Forms.show(this, I18n.t("Pay ") + CompanyLabels.money(o.total), I18n.t("Send prompt"), f, v ->
                Ui.watch(this, svm.pay(o.id, v.get("phone")), b.progress, r -> {
                    Ui.toast(this, I18n.t("Check your phone and enter your M-Pesa PIN"));
                    load();
                }));
    }
}

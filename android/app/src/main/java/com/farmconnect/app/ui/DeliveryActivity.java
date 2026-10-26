package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.DeliveryJob;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Delivery jobs. A buyer sees the deliveries they asked for, a farmer the deliveries collecting their produce, a transporter
 * the jobs sent to their company (accept, collect, deliver, mark the fee received), and the super admin sees them all.
 * Fees are paid between buyer and transporter; FarmConnect only records them.
 */
public class DeliveryActivity extends BaseListActivity {
    private static String role() { return Session.role() == null ? "" : Session.role(); }

    @Override protected String screenTitle() { return "COMPANY".equals(role()) ? I18n.t("Delivery jobs") : I18n.t("Deliveries"); }
    @Override protected String emptyText() { return I18n.t("No deliveries yet"); }
    @Override protected String[][] chips() {
        return new String[][]{{"", "All"}, {"REQUESTED", "Waiting"}, {"ACCEPTED", "Accepted"}, {"PICKED_UP", "On the way"}, {"DELIVERED", "Delivered"}};
    }

    static String statusName(String s) {
        switch (s == null ? "" : s) {
            case "REQUESTED": return I18n.t("Waiting");
            case "ACCEPTED": return I18n.t("Accepted");
            case "PICKED_UP": return I18n.t("On the way");
            case "DELIVERED": return I18n.t("Delivered");
            case "DECLINED": return I18n.t("Declined");
            default: return I18n.t("Cancelled");
        }
    }

    static int statusColor(String s) {
        switch (s == null ? "" : s) {
            case "REQUESTED": return 0xFFF9A825;
            case "ACCEPTED": return 0xFF1E88E5;
            case "PICKED_UP": return 0xFF8E24AA;
            case "DELIVERED": return 0xFF2E7D32;
            default: return 0xFF757575;
        }
    }

    private static String money(Double v) { return v == null ? "-" : Ui.kes(v); }

    @Override protected void load() {
        if (vm == null) return;
        Ui.watch(this, vm.deliveryJobs(chip.isEmpty() ? null : chip), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            boolean transporter = "COMPANY".equals(role());
            for (DeliveryJob j : list) {
                String who = transporter ? j.buyerName : j.transporterName;
                rows.add(Row.of("🚚", I18n.t("Order #") + j.orderId + " · " + who)
                        .sub((j.pickupPlace == null ? "" : j.pickupPlace + " → ") + (j.dropAddress == null ? "" : j.dropAddress))
                        .meta(j.fee == null ? I18n.t("Fee to be confirmed") : money(j.fee) + (j.distanceKm == null ? "" : " · " + j.distanceKm + " km")
                                + (j.feePaid ? " · " + I18n.t("Fee paid") : ""))
                        .badge(statusName(j.status), statusColor(j.status))
                        .click(() -> showJob(j)));
            }
            show(rows, list.size() + I18n.t(" deliveries"));
        });
    }

    private void showJob(DeliveryJob j) {
        String r = role();
        boolean transporter = "COMPANY".equals(r), buyer = "BUYER".equals(r);
        Sheet s = Sheet.of(this, I18n.t("Order #") + j.orderId).badge(statusName(j.status), statusColor(j.status))
                .field(I18n.t("Collect from"), j.pickupPlace)
                .field(I18n.t("Deliver to"), j.dropAddress)
                .field(I18n.t("Transporter"), j.transporterName + (j.transporterPhone == null ? "" : " · " + j.transporterPhone))
                .field(I18n.t("Buyer"), j.buyerName + (j.buyerPhone == null ? "" : " · " + j.buyerPhone))
                .field(I18n.t("Farmer"), j.farmerName + (j.farmerPhone == null ? "" : " · " + j.farmerPhone))
                .field(I18n.t("Distance"), j.distanceKm == null ? null : j.distanceKm + " km")
                .note(j.note);
        if (j.fee != null) s.strong(I18n.t("Delivery fee"), Ui.kes(j.fee));
        if (j.baseFee != null && j.perKmRate != null)
            s.field(I18n.t("How it was worked out"), Ui.kes(j.baseFee) + " + " + Ui.kes(j.perKmRate) + I18n.t(" per km"));
        s.field(I18n.t("Reason"), j.declineReason);
        s.note(I18n.t("The delivery fee is paid between the buyer and the transporter. FarmConnect only records it."));
        if (j.feePaid) s.field(I18n.t("Fee"), I18n.t("Paid"));

        if (transporter) {
            if ("REQUESTED".equals(j.status)) {
                s.primary(I18n.t("Accept this job"), () -> accept(j));
                s.danger(I18n.t("Decline"), () -> decline(j));
            }
            if ("ACCEPTED".equals(j.status)) s.primary(I18n.t("I have collected the order"), () -> step(vm.deliveryPickup(j.id), I18n.t("Marked as collected")));
            if ("PICKED_UP".equals(j.status)) s.primary(I18n.t("I have delivered the order"), () -> step(vm.deliveryDeliver(j.id), I18n.t("Marked as delivered")));
            if ("DELIVERED".equals(j.status) && !j.feePaid) s.primary(I18n.t("I received the fee"), () -> step(vm.deliveryFeeReceived(j.id), I18n.t("Fee recorded as paid")));
        }
        if (buyer && ("REQUESTED".equals(j.status) || "ACCEPTED".equals(j.status)))
            s.danger(I18n.t("Cancel delivery"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Cancel this delivery?"))
                    .setPositiveButton(I18n.t("Cancel delivery"), (x, y) -> step(vm.deliveryCancel(j.id), I18n.t("Delivery cancelled")))
                    .setNegativeButton(I18n.t("Keep"), null).show());
        s.show();
    }

    private void step(androidx.lifecycle.LiveData<com.farmconnect.app.data.Resource<DeliveryJob>> call, String done) {
        Ui.watch(this, call, b.progress, r -> { Ui.toast(this, done); load(); });
    }

    private void accept(DeliveryJob j) {
        if (!j.needsDistance) { step(vm.deliveryAccept(j.id, null), I18n.t("Job accepted")); return; }
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.number("km", I18n.t("Distance in km from the farm to the buyer"), true));
        Forms.show(this, I18n.t("Accept this job"), I18n.t("Accept"), f, v -> {
            double km;
            try { km = Double.parseDouble(v.get("km")); } catch (Exception e) { Ui.toast(this, I18n.t("Enter a number")); return; }
            step(vm.deliveryAccept(j.id, km), I18n.t("Job accepted"));
        });
    }

    private void decline(DeliveryJob j) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("reason", I18n.t("Reason (optional)"), false));
        Forms.show(this, I18n.t("Decline"), I18n.t("Decline"), f, v -> step(vm.deliveryDecline(j.id, v.get("reason")), I18n.t("Job declined")));
    }

    /** Super admin: the price list. Base fee + rate per km, and how much longer roads are than a straight line. */
    static void ratesDialog(androidx.appcompat.app.AppCompatActivity a, com.farmconnect.app.vm.CommunityViewModel vm) {
        Ui.watch(a, vm.deliveryRates(), null, r -> {
            List<Forms.Field> f = new ArrayList<>();
            f.add(Forms.number("base", I18n.t("Base fee (KES)"), true).value(fmt(r.baseFee)));
            f.add(Forms.number("perKm", I18n.t("Rate per km (KES)"), true).value(fmt(r.perKmRate)));
            f.add(Forms.number("factor", I18n.t("Road factor (1.3 = roads are 30% longer than a straight line)"), true).value(fmt(r.roadFactor)));
            f.add(Forms.number("max", I18n.t("Longest delivery (km)"), true).value(fmt(r.maxKm)));
            Forms.show(a, I18n.t("Delivery rates"), I18n.t("Save"), f, v -> {
                double base, perKm, factor, max;
                try {
                    base = Double.parseDouble(v.get("base")); perKm = Double.parseDouble(v.get("perKm"));
                    factor = Double.parseDouble(v.get("factor")); max = Double.parseDouble(v.get("max"));
                } catch (Exception e) { Ui.toast(a, I18n.t("Enter a number")); return; }
                Ui.watch(a, vm.setDeliveryRates(base, perKm, factor, max), null, x -> Ui.toast(a, I18n.t("Delivery rates saved")));
            });
        });
    }

    private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v); }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.farmconnect.app.data.Models.DeliveryOptions;
import com.farmconnect.app.data.Models.DeliveryQuote;
import com.farmconnect.app.data.Models.Transporter;
import com.farmconnect.app.util.Ui;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.ArrayList;
import java.util.List;

/**
 * A buyer chooses a transporter for an order. The fee is worked out from the farm to the buyer's current location
 * (base fee + rate per km, set by the super admin). Without a location the transporter confirms the distance instead.
 */
public class ArrangeDeliveryActivity extends BaseListActivity {
    private boolean ready;
    private Double lat, lng;
    private long orderId;

    private final ActivityResultLauncher<String> permission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) fetchLocation(); else { ready = true; load(); }
            });

    @Override protected String screenTitle() { return I18n.t("Choose a transporter"); }
    @Override protected String emptyText() { return I18n.t("No verified transporters serve this area yet"); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        orderId = getIntent().getLongExtra("orderId", -1);
        if (orderId < 0) { finish(); return; }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchLocation();
        else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        b.progress.setVisibility(android.view.View.VISIBLE);
        LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) { lat = loc.getLatitude(); lng = loc.getLongitude(); }
                    ready = true;
                    load();
                })
                .addOnFailureListener(e -> { ready = true; load(); });
    }

    @Override protected void load() {
        if (!ready || vm == null || orderId < 0) return;
        Ui.watch(this, vm.deliveryOptions(orderId, lat, lng), b.progress, o -> {
            if (o.activeJobId != null) {
                Ui.toast(this, I18n.t("This order already has a delivery"));
                startActivity(new Intent(this, DeliveryActivity.class));
                finish();
                return;
            }
            List<Row> rows = new ArrayList<>();
            for (Transporter t : o.transporters) {
                rows.add(Row.of("🚚", t.name)
                        .sub((t.phone == null ? "" : t.phone) + (t.operatingCounties == null || t.operatingCounties.isEmpty() ? "" : " · " + t.operatingCounties))
                        .meta(t.delivered + I18n.t(" deliveries done"))
                        .click(() -> confirm(t, o.quote)));
            }
            show(rows, quoteText(o.quote));
        });
    }

    private String quoteText(DeliveryQuote q) {
        if (q.fee != null) return I18n.t("About ") + Ui.kes(q.fee) + " · " + q.distanceKm + " km";
        return I18n.t("Fee: ") + Ui.kes(q.baseFee) + " + " + Ui.kes(q.perKmRate) + I18n.t(" per km. The transporter confirms the distance.");
    }

    private void confirm(Transporter t, DeliveryQuote q) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("note", I18n.t("Note for the transporter (optional)"), false));
        Forms.show(this, t.name + " · " + quoteText(q), I18n.t("Send request"), f, v ->
                Ui.watch(this, vm.deliveryRequest(orderId, t.id, lat, lng, v.get("note")), b.progress, j -> {
                    Ui.toast(this, I18n.t("Request sent. You will get an alert when the transporter answers."));
                    startActivity(new Intent(this, DeliveryActivity.class));
                    finish();
                }));
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.ReviewDetail;
import com.farmconnect.app.databinding.ActivityReviewDetailBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.ReviewViewModel;
import java.util.Locale;

/** Reviewer checks the farmer's details and photos, then starts review / approves / rejects. */
public class ReviewDetailActivity extends AppCompatActivity {
    private ActivityReviewDetailBinding b;
    private ReviewViewModel vm;
    private long id;
    private boolean imagesLoaded;
    private ReviewDetail current;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle(I18n.t("Review farmer"));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b = ActivityReviewDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(ReviewViewModel.class);
        id = getIntent().getLongExtra("id", -1);

        b.btnStart.setOnClickListener(v ->
                Ui.watch(this, vm.start(id), b.progress, d -> {
                    Ui.toast(this, I18n.t("Review started"));
                    bind(d);
                }));
        b.btnApprove.setOnClickListener(v -> confirmApprove());
        b.btnReject.setOnClickListener(v -> askReject());
        b.btnMap.setOnClickListener(v -> openMap());

        Ui.watch(this, vm.detail(id), b.progress, this::bind);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private static String v(String s) { return s == null || s.isEmpty() ? "-" : s; }

    private void bind(ReviewDetail d) {
        current = d;
        b.tvFullName.setText(v(d.fullName));
        Ui.styleVerification(b.tvStatus, d.status);
        b.tvNationalId.setText(I18n.t("National ID: ") + v(d.nationalId));
        b.tvPhone.setText(I18n.t("Phone: ") + v(d.phone) + (d.phoneVerified ? I18n.t("  ✓ OTP verified") : I18n.t("  (not verified)")));
        b.tvAccount.setText(I18n.t("Account: ") + v(d.accountName) + " · " + v(d.accountEmail));
        b.tvLocation.setText(v(d.county) + I18n.t(" County · ") + v(d.subCounty) + I18n.t(" · Ward: ") + v(d.ward) + I18n.t(" · Village: ") + v(d.village));
        boolean hasGps = d.farmLat != null && d.farmLng != null;
        b.tvGps.setText(hasGps ? String.format(Locale.US, I18n.t("Farm GPS: %.6f, %.6f"), d.farmLat, d.farmLng) : I18n.t("Farm GPS: -"));
        b.btnMap.setVisibility(hasGps ? View.VISIBLE : View.GONE);
        b.tvFarm.setText(I18n.t("Farming: ") + v(d.farmingType) + " · " + v(d.mainProduce)
                + " · " + (d.farmSize == null ? "-" : d.farmSize + I18n.t(" acres")));
        b.tvReviewed.setText(d.reviewedByName == null ? "" : I18n.t("Reviewer: ") + d.reviewedByName
                + (d.reviewedAt == null ? "" : " · " + Ui.dateTime(d.reviewedAt)));
        b.tvReason.setText(d.rejectionReason == null ? "" : I18n.t("Rejection reason: ") + d.rejectionReason);

        b.btnStart.setVisibility("PENDING".equals(d.status) ? View.VISIBLE : View.GONE);
        boolean reviewing = "UNDER_REVIEW".equals(d.status);
        b.btnApprove.setVisibility(reviewing ? View.VISIBLE : View.GONE);
        b.btnReject.setVisibility(reviewing ? View.VISIBLE : View.GONE);

        if (!imagesLoaded) {
            imagesLoaded = true;
            if (d.hasIdFront) Ui.watch(this, vm.doc(id, "ID_FRONT"), null, bm -> b.ivIdFront.setImageBitmap(bm));
            if (d.hasIdBack) Ui.watch(this, vm.doc(id, "ID_BACK"), null, bm -> b.ivIdBack.setImageBitmap(bm));
            if (d.hasSelfie) Ui.watch(this, vm.doc(id, "SELFIE"), null, bm -> b.ivSelfie.setImageBitmap(bm));
            if (d.hasProof) Ui.watch(this, vm.doc(id, "PROOF"), null, bm -> b.ivProof.setImageBitmap(bm));
        }
    }

    private void confirmApprove() {
        String name = current == null ? "" : v(current.fullName);
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("Approve this farmer?"))
                .setMessage(I18n.t("Confirm that:\n• the name \"") + name + I18n.t("\" and the ID number match the ID photos\n")
                        + I18n.t("• the selfie matches the ID photo\n• the farm proof and location look genuine"))
                .setPositiveButton(I18n.t("Approve"), (x, y) ->
                        Ui.watch(this, vm.approve(id), b.progress, d -> {
                            Ui.toast(this, I18n.t("Farmer verified ✓"));
                            bind(d);
                        }))
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    private void askReject() {
        EditText reason = new EditText(this);
        reason.setHint(I18n.t("Reason shown to the farmer (min 5 characters)"));
        reason.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("Reject verification"))
                .setView(reason)
                .setPositiveButton(I18n.t("Reject"), (x, y) -> {
                    String r = reason.getText().toString().trim();
                    if (r.length() < 5) { Ui.toast(this, I18n.t("Please give a reason")); return; }
                    Ui.watch(this, vm.reject(id, r), b.progress, d -> {
                        Ui.toast(this, I18n.t("Rejected. The farmer can fix and resubmit"));
                        bind(d);
                    });
                })
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    private void openMap() {
        if (current == null || current.farmLat == null || current.farmLng == null) return;
        String uri = String.format(Locale.US, I18n.t("geo:%f,%f?q=%f,%f(Farm)"), current.farmLat, current.farmLng,
                current.farmLat, current.farmLng);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, I18n.t("No maps app installed"));
        }
    }
}
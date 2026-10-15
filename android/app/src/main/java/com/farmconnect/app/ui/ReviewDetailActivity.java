package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Models.ReviewDetail;
import com.farmconnect.app.databinding.ActivityReviewDetailBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.ReviewViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
        b = ActivityReviewDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(ReviewViewModel.class);
        id = getIntent().getLongExtra("id", -1);

        b.btnBack.setOnClickListener(v -> finish());
        b.btnStart.setOnClickListener(v ->
                Ui.watch(this, vm.start(id), b.progress, d -> {
                    Ui.toast(this, I18n.t("Review started"));
                    bind(d);
                }));
        b.btnApprove.setOnClickListener(v -> confirmApprove());
        b.btnReject.setOnClickListener(v -> askReject());
        b.btnMap.setOnClickListener(v -> openMap());

        // Tap any document photo to open it full screen.
        for (ImageView iv : new ImageView[]{b.ivIdFront, b.ivIdBack, b.ivSelfie, b.ivProof}) {
            iv.setOnClickListener(v -> showImage((ImageView) v));
        }

        Ui.watch(this, vm.detail(id), b.progress, this::bind);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private static String v(String s) { return s == null || s.isEmpty() ? "-" : s; }

    private void bind(ReviewDetail d) {
        current = d;
        b.tvInitial.setText(Ui.initial(d.fullName));
        b.tvFullName.setText(v(d.fullName));
        b.tvSubmitted.setText(d.submittedAt == null ? "" : getString(R.string.off_submitted) + " " + Ui.dateTime(d.submittedAt));
        Ui.styleVerification(b.tvStatus, d.status);

        b.tvNationalId.setText(v(d.nationalId));
        b.tvPhone.setText(v(d.phone));
        int pc = d.phoneVerified ? 0xFF2E7D32 : 0xFFF9A825;
        b.tvPhoneStatus.setText(d.phoneVerified ? R.string.off_otp_ok : R.string.off_otp_no);
        b.tvPhoneStatus.setTextColor(d.phoneVerified ? pc : 0xFFB26A00);
        b.tvPhoneStatus.setBackgroundTintList(ColorStateList.valueOf((pc & 0x00FFFFFF) | 0x26000000));
        b.tvAccount.setText(v(d.accountName));
        b.tvAccountEmail.setText(v(d.accountEmail));

        b.tvCounty.setText(v(d.county));
        b.tvSubCounty.setText(v(d.subCounty));
        b.tvWard.setText(v(d.ward));
        b.tvVillage.setText(v(d.village));
        boolean hasGps = d.farmLat != null && d.farmLng != null;
        b.tvGps.setText(hasGps ? String.format(Locale.US, "%.6f, %.6f", d.farmLat, d.farmLng) : "-");
        b.btnMap.setVisibility(hasGps ? View.VISIBLE : View.GONE);

        b.tvFarmingType.setText(v(d.farmingType));
        b.tvProduce.setText(v(d.mainProduce));
        b.tvFarmSize.setText(d.farmSize == null ? "-" : d.farmSize + I18n.t(" acres"));

        boolean hasReason = d.rejectionReason != null && !d.rejectionReason.isEmpty();
        boolean hasOutcome = d.reviewedByName != null || hasReason;
        b.cardOutcome.setVisibility(hasOutcome ? View.VISIBLE : View.GONE);
        b.tvReviewed.setText(d.reviewedByName == null ? "-" : d.reviewedByName
                + (d.reviewedAt == null ? "" : " · " + Ui.dateTime(d.reviewedAt)));
        b.rowReason.setVisibility(hasReason ? View.VISIBLE : View.GONE);
        b.tvReason.setText(hasReason ? d.rejectionReason : "");

        boolean pending = "PENDING".equals(d.status);
        boolean reviewing = "UNDER_REVIEW".equals(d.status);
        b.btnStart.setVisibility(pending ? View.VISIBLE : View.GONE);
        b.rowDecision.setVisibility(reviewing ? View.VISIBLE : View.GONE);
        b.actionBar.setVisibility(pending || reviewing ? View.VISIBLE : View.GONE);

        b.tvIdFrontMissing.setVisibility(d.hasIdFront ? View.GONE : View.VISIBLE);
        b.tvIdBackMissing.setVisibility(d.hasIdBack ? View.GONE : View.VISIBLE);
        b.tvSelfieMissing.setVisibility(d.hasSelfie ? View.GONE : View.VISIBLE);
        b.tvProofMissing.setVisibility(d.hasProof ? View.GONE : View.VISIBLE);

        if (!imagesLoaded) {
            imagesLoaded = true;
            if (d.hasIdFront) Ui.watch(this, vm.doc(id, "ID_FRONT"), null, bm -> b.ivIdFront.setImageBitmap(bm));
            if (d.hasIdBack) Ui.watch(this, vm.doc(id, "ID_BACK"), null, bm -> b.ivIdBack.setImageBitmap(bm));
            if (d.hasSelfie) Ui.watch(this, vm.doc(id, "SELFIE"), null, bm -> b.ivSelfie.setImageBitmap(bm));
            if (d.hasProof) Ui.watch(this, vm.doc(id, "PROOF"), null, bm -> b.ivProof.setImageBitmap(bm));
        }
    }

    /** Full-screen photo viewer: pinch to zoom, drag to pan, double-tap to reset. */
    private void showImage(ImageView src) {
        Drawable dr = src.getDrawable();
        if (dr == null) return;
        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(dr);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final float[] st = {1f, 0f, 0f, 0f, 0f}; // scale, translateX, translateY, lastX, lastY
        ScaleGestureDetector sg = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector det) {
                st[0] = Math.max(1f, Math.min(6f, st[0] * det.getScaleFactor()));
                iv.setScaleX(st[0]);
                iv.setScaleY(st[0]);
                return true;
            }
        });
        GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) {
                st[0] = 1f; st[1] = 0f; st[2] = 0f;
                iv.setScaleX(1f); iv.setScaleY(1f); iv.setTranslationX(0f); iv.setTranslationY(0f);
                return true;
            }
        });
        iv.setOnTouchListener((view, e) -> {
            sg.onTouchEvent(e);
            gd.onTouchEvent(e);
            if (e.getPointerCount() == 1 && !sg.isInProgress() && st[0] > 1f) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { st[3] = e.getX(); st[4] = e.getY(); }
                else if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    st[1] += e.getX() - st[3]; st[2] += e.getY() - st[4];
                    st[3] = e.getX(); st[4] = e.getY();
                    iv.setTranslationX(st[1]); iv.setTranslationY(st[2]);
                }
            } else if (e.getActionMasked() == MotionEvent.ACTION_POINTER_UP || e.getActionMasked() == MotionEvent.ACTION_UP) {
                st[3] = e.getX(); st[4] = e.getY();
            }
            return true;
        });

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(0xFFFFFFFF);
        close.setTextSize(20);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundResource(R.drawable.bg_icon_glass);
        close.setContentDescription(I18n.t("Close"));
        close.setOnClickListener(x -> dlg.dismiss());
        int sz = Cards.dp(this, 44), m = Cards.dp(this, 16);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(sz, sz, Gravity.TOP | Gravity.END);
        cp.setMargins(m, m * 2, m, 0);
        root.addView(close, cp);

        dlg.setContentView(root);
        dlg.show();
    }

    private void confirmApprove() {
        String name = current == null ? "" : v(current.fullName);
        new MaterialAlertDialogBuilder(this)
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
        reason.setMinLines(3);
        reason.setGravity(Gravity.TOP | Gravity.START);
        reason.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        FrameLayout box = new FrameLayout(this);
        int pad = Cards.dp(this, 20);
        box.setPadding(pad, Cards.dp(this, 8), pad, 0);
        box.addView(reason, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle(I18n.t("Reject verification"))
                .setView(box)
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
        String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(Farm)", current.farmLat, current.farmLng,
                current.farmLat, current.farmLng);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, I18n.t("No maps app installed"));
        }
    }
}

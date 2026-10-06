package com.farmconnect.app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.DraftRequest;
import com.farmconnect.app.data.Models.VerificationResponse;
import com.farmconnect.app.databinding.ActivityVerificationBinding;
import com.farmconnect.app.util.ImageUtil;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.VerificationViewModel;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.Locale;

/** Farmer fills in the verification form, uploads photos, verifies phone by OTP, then submits. */
public class VerificationActivity extends AppCompatActivity {
    private static final String[] SUB = {"Select sub-county", "Cherangany", "Endebess", "Kiminini", "Kwanza", "Saboti"};
    private static final String[] TYPE_LABELS = {"Select type of farming", "Crop farming", "Livestock", "Poultry",
            "Mixed farming", "Aquaculture (fish)", "Other"};
    private static final String[] TYPE_CODES = {null, "CROP", "LIVESTOCK", "POULTRY", "MIXED", "AQUACULTURE", "OTHER"};

    private ActivityVerificationBinding b;
    private VerificationViewModel vm;
    private String pendingType;
    private CountDownTimer timer;

    private final ActivityResultLauncher<String> picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> { if (uri != null) upload(uri); });
    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) fetchLocation();
                else Ui.toast(this, "Location permission denied. Type the coordinates instead.");
            });

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTitle("Verify your account");
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        b = ActivityVerificationBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(VerificationViewModel.class);

        b.spSubCounty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SUB));
        b.spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TYPE_LABELS));

        b.btnIdFront.setOnClickListener(v -> pick("ID_FRONT"));
        b.btnIdBack.setOnClickListener(v -> pick("ID_BACK"));
        b.btnSelfie.setOnClickListener(v -> pick("SELFIE"));
        b.btnProof.setOnClickListener(v -> pick("PROOF"));

        b.btnSendOtp.setOnClickListener(v -> {
            String phone = text(b.etPhone);
            if (phone.isEmpty()) { Ui.toast(this, "Enter your phone number"); return; }
            Ui.watch(this, vm.sendOtp(phone), b.progress, x -> {
                Ui.toast(this, "Code sent. Check your SMS");
                startCooldown();
            });
        });
        b.btnVerifyOtp.setOnClickListener(v -> {
            String phone = text(b.etPhone), code = text(b.etOtp);
            if (phone.isEmpty() || code.isEmpty()) { Ui.toast(this, "Enter phone and the 6-digit code"); return; }
            Ui.watch(this, vm.verifyOtp(phone, code), b.progress, r -> {
                Ui.toast(this, "Phone verified ✓");
                b.etOtp.setText("");
                updateFlags(r);
            });
        });

        b.btnLocate.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) fetchLocation();
            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });

        b.tvTerms.setOnClickListener(v -> showTerms());
        b.btnSave.setOnClickListener(v ->
                Ui.watch(this, vm.saveDraft(collect()), b.progress, r -> {
                    Ui.toast(this, "Draft saved");
                    updateFlags(r);
                }));
        b.btnSubmit.setOnClickListener(v ->
                Ui.watch(this, vm.saveDraft(collect()), b.progress, saved ->
                        Ui.watch(this, vm.submit(), b.progress, r -> {
                            Ui.toast(this, "Submitted. We will review your details");
                            updateFlags(r);
                        })));

        Ui.watch(this, vm.mine(), b.progress, r -> {
            fillForm(r);
            updateFlags(r);
        });
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    // ---------- form <-> data ----------

    private static String text(android.widget.EditText e) { return e.getText().toString().trim(); }

    private static Double num(android.widget.EditText e) {
        try { return Double.parseDouble(e.getText().toString().trim()); } catch (Exception ex) { return null; }
    }

    private DraftRequest collect() {
        DraftRequest d = new DraftRequest();
        d.fullName = text(b.etFullName);
        String id = text(b.etNationalId);
        if (!id.isEmpty()) d.nationalId = id;
        int sc = b.spSubCounty.getSelectedItemPosition();
        if (sc > 0) d.subCounty = SUB[sc];
        d.ward = text(b.etWard);
        d.village = text(b.etVillage);
        d.farmLat = num(b.etLat);
        d.farmLng = num(b.etLng);
        int t = b.spType.getSelectedItemPosition();
        if (t > 0) d.farmingType = TYPE_CODES[t];
        d.mainProduce = text(b.etProduce);
        d.farmSize = num(b.etSize);
        d.termsAccepted = b.cbTerms.isChecked();
        return d;
    }

    private void fillForm(VerificationResponse v) {
        if (v.fullName != null) b.etFullName.setText(v.fullName);
        if (v.maskedNationalId != null) b.tilNationalId.setHelperText("Saved: " + v.maskedNationalId + " (leave empty to keep)");
        if (v.phone != null) b.etPhone.setText(v.phone);
        for (int i = 1; i < SUB.length; i++) if (SUB[i].equals(v.subCounty)) b.spSubCounty.setSelection(i);
        if (v.ward != null) b.etWard.setText(v.ward);
        if (v.village != null) b.etVillage.setText(v.village);
        if (v.farmLat != null) b.etLat.setText(String.format(Locale.US, "%.6f", v.farmLat));
        if (v.farmLng != null) b.etLng.setText(String.format(Locale.US, "%.6f", v.farmLng));
        for (int i = 1; i < TYPE_CODES.length; i++) if (TYPE_CODES[i].equals(v.farmingType)) b.spType.setSelection(i);
        if (v.mainProduce != null) b.etProduce.setText(v.mainProduce);
        if (v.farmSize != null) b.etSize.setText(v.farmSize == Math.floor(v.farmSize) ? String.valueOf(v.farmSize.longValue()) : String.valueOf(v.farmSize));
        b.cbTerms.setChecked(v.termsAccepted);
    }

    private void updateFlags(VerificationResponse v) {
        flag(b.tvIdFront, b.btnIdFront, v.hasIdFront);
        flag(b.tvIdBack, b.btnIdBack, v.hasIdBack);
        flag(b.tvSelfie, b.btnSelfie, v.hasSelfie);
        flag(b.tvProof, b.btnProof, v.hasProof);
        b.tvPhoneStatus.setText(v.phoneVerified ? "✓ Verified: " + v.phone : "Not verified");
        b.tvPhoneStatus.setTextColor(v.phoneVerified ? 0xFF2E7D32 : 0xFFC62828);

        String title, msg;
        int bg;
        switch (v.status == null ? "DRAFT" : v.status) {
            case "PENDING":
                bg = 0xFFE3F2FD; title = "Pending verification";
                msg = "We received your details. A reviewer will pick them up soon."; break;
            case "UNDER_REVIEW":
                bg = 0xFFEDE7F6; title = "Under review";
                msg = "A reviewer is checking your details now."; break;
            case "VERIFIED":
                bg = 0xFFE3F1E0; title = "✓ Verified Farmer";
                msg = "Your identity and farm are verified. Buyers can see your badge."; break;
            case "REJECTED":
                bg = 0xFFFDECEA; title = "Verification rejected";
                msg = (v.rejectionReason == null ? "" : "Reason: " + v.rejectionReason + "\n") + "Fix the issues and submit again."; break;
            default:
                bg = 0xFFFFF4D6; title = "Not verified yet";
                msg = "Complete every section below, then submit. Your ID details stay private.";
        }
        b.cardStatus.setCardBackgroundColor(bg);
        b.tvStatusTitle.setText(title);
        b.tvStatusMsg.setText(msg);
        setEditable("DRAFT".equals(v.status) || "REJECTED".equals(v.status));
    }

    private void flag(TextView tv, android.widget.Button btn, boolean has) {
        tv.setText(has ? "✓ Uploaded" : "Not uploaded");
        tv.setTextColor(has ? 0xFF2E7D32 : 0xFFC62828);
        btn.setText(has ? "Replace" : "Upload");
    }

    private void setEditable(boolean on) {
        View[] views = {b.etFullName, b.etNationalId, b.btnIdFront, b.btnIdBack, b.btnSelfie, b.etPhone, b.btnSendOtp,
                b.etOtp, b.btnVerifyOtp, b.spSubCounty, b.etWard, b.etVillage, b.etLat, b.etLng, b.btnLocate,
                b.spType, b.etProduce, b.etSize, b.btnProof, b.cbTerms};
        for (View v : views) v.setEnabled(on);
        b.btnSave.setVisibility(on ? View.VISIBLE : View.GONE);
        b.btnSubmit.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    // ---------- actions ----------

    private void pick(String type) {
        pendingType = type;
        picker.launch("image/*");
    }

    private void upload(Uri uri) {
        final String type = pendingType;
        b.progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                byte[] data = ImageUtil.compress(this, uri);
                runOnUiThread(() -> Ui.watch(this, vm.uploadDoc(type, data), b.progress, r -> {
                    Ui.toast(this, "Photo uploaded");
                    updateFlags(r);
                }));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    b.progress.setVisibility(View.GONE);
                    Ui.toast(this, "Could not read that image");
                });
            }
        }).start();
    }

    private void startCooldown() {
        b.btnSendOtp.setEnabled(false);
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(60_000, 1_000) {
            @Override public void onTick(long ms) { b.btnSendOtp.setText("Resend in " + (ms / 1000) + "s"); }
            @Override public void onFinish() { b.btnSendOtp.setText("Send code"); b.btnSendOtp.setEnabled(true); }
        }.start();
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        b.progress.setVisibility(View.VISIBLE);
        LocationServices.getFusedLocationProviderClient(this)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    b.progress.setVisibility(View.GONE);
                    if (loc == null) {
                        Ui.toast(this, "Could not get location. Turn on GPS or type the coordinates.");
                        return;
                    }
                    b.etLat.setText(String.format(Locale.US, "%.6f", loc.getLatitude()));
                    b.etLng.setText(String.format(Locale.US, "%.6f", loc.getLongitude()));
                })
                .addOnFailureListener(e -> {
                    b.progress.setVisibility(View.GONE);
                    Ui.toast(this, "Could not get location");
                });
    }

    private void showTerms() {
        new AlertDialog.Builder(this)
                .setTitle("Verification & privacy terms")
                .setMessage("• We collect your name, National ID number and photos, phone number, farm location and "
                        + "farm details only to confirm you are a real farmer in Trans Nzoia County.\n\n"
                        + "• Only authorized FarmConnect administrators and County Agricultural Officers can see these details.\n\n"
                        + "• Your ID number and photos are never shown to buyers or the public. Buyers only see a "
                        + "\"Verified Farmer\" badge.\n\n"
                        + "• Your ID number is stored encrypted. Your details are not sold or shared for advertising.\n\n"
                        + "• Giving false information can lead to rejection or removal of your account.\n\n"
                        + "• You can ask us to correct or delete your information.")
                .setPositiveButton("OK", null)
                .show();
    }
}
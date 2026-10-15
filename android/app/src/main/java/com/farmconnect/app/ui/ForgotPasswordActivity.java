package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.databinding.ActivityForgotPasswordBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityForgotPasswordBinding b = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);

        String prefill = getIntent().getStringExtra("email");
        if (prefill != null) b.etEmail.setText(prefill);

        // Step 1: ask for the code
        b.btnSend.setOnClickListener(v -> {
            String email = b.etEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Ui.toast(this, I18n.t("Enter a valid email"));
                return;
            }
            Ui.watch(this, vm.forgotPassword(email), b.progress, r -> {
                Ui.toast(this, I18n.t("If that email is registered, we sent a 6-digit code."));
                b.groupReset.setVisibility(View.VISIBLE);
                b.btnSend.setText(I18n.t("Resend code"));
            });
        });

        // Step 2: code + new password + confirm
        b.btnReset.setOnClickListener(v -> {
            String email = b.etEmail.getText().toString().trim();
            String code = b.etCode.getText().toString().trim();
            String pass = b.etNewPassword.getText().toString();
            String confirm = b.etConfirm.getText().toString();
            if (code.length() != 6) { Ui.toast(this, I18n.t("Enter the 6-digit code")); return; }
            if (pass.length() < 6) { Ui.toast(this, I18n.t("Password min 6 characters")); return; }
            if (!pass.equals(confirm)) { Ui.toast(this, I18n.t("Passwords do not match")); return; }
            Ui.watch(this, vm.resetPassword(email, code, pass), b.progress, r -> {
                Ui.toast(this, I18n.t("Password updated. Please log in."));
                finish();
            });
        });

        b.tvBack.setOnClickListener(v -> finish());
    }
}

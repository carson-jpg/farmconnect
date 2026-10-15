package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityLoginBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    private AuthViewModel vm;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Session.isLoggedIn()) { Ui.home(this); return; }
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        b.btnLogin.setOnClickListener(v -> {
            String email = b.etEmail.getText().toString().trim();
            String pass = b.etPassword.getText().toString();
            if (email.isEmpty() || pass.isEmpty()) { Ui.toast(this, I18n.t("Enter email and password")); return; }
            Ui.watch(this, vm.login(email, pass), b.progress, r -> {
                Session.save(r);
                Ui.home(this);
            });
        });
        b.tvLang.setOnClickListener(v -> com.farmconnect.app.util.I18n.chooseLanguage(this));
        b.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        b.tvForgot.setOnClickListener(v -> {
            Intent i = new Intent(this, ForgotPasswordActivity.class);
            i.putExtra("email", b.etEmail.getText().toString().trim());
            startActivity(i);
        });
        setupGoogle();
    }

    // ---------------------------------------------------------------- Google sign-in
    private void setupGoogle() {
        String webClientId = getString(R.string.google_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
            try {
                GoogleSignInAccount acct = GoogleSignIn.getSignedInAccountFromIntent(res.getData()).getResult(ApiException.class);
                String idToken = acct.getIdToken();
                if (idToken == null) { Ui.toast(this, I18n.t("Google sign-in failed")); return; }
                googleLogin(idToken, null);
            } catch (ApiException e) {
                if (e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED)
                    Ui.toast(this, I18n.t("Google sign-in failed") + " (" + e.getStatusCode() + ")");
            }
        });

        b.btnGoogle.setOnClickListener(v -> {
            if (webClientId.startsWith("YOUR_")) { Ui.toast(this, I18n.t("Google sign-in is not set up yet")); return; }
            // sign out first so the account picker always shows
            googleClient.signOut().addOnCompleteListener(t -> googleLauncher.launch(googleClient.getSignInIntent()));
        });
    }

    /** role == null for a first try; if the server says NEW_ACCOUNT we ask Buyer/Farmer and call again with the same token. */
    private void googleLogin(String idToken, String role) {
        vm.google(idToken, role).observe(this, r -> {
            b.progress.setVisibility(r.status == Resource.Status.LOADING ? android.view.View.VISIBLE : android.view.View.GONE);
            if (r.status == Resource.Status.SUCCESS) {
                Session.save(r.data);
                Ui.home(this);
            } else if (r.status == Resource.Status.ERROR) {
                if ("NEW_ACCOUNT".equals(r.message)) askRole(idToken);
                else Ui.toast(this, r.message);
            }
        });
    }

    private void askRole(String idToken) {
        String[] labels = { getString(R.string.t_65fba9ec), getString(R.string.t_8bf7aa45) }; // Buyer, Farmer
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("Create your account as"))
                .setItems(labels, (d, which) -> googleLogin(idToken, which == 1 ? "FARMER" : "BUYER"))
                .show();
    }
}

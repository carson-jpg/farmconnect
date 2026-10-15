package com.farmconnect.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.I18n;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;

/**
 * Google sign-in / sign-up, shared by the Login and Register screens.
 * Create it in onCreate (it registers an activity-result launcher), then call {@link #start}
 * from the button's click listener.
 */
public class GoogleAuth {
    private final AppCompatActivity act;
    private final AuthViewModel vm;
    private final View progress;
    private final String webClientId;
    private final GoogleSignInClient client;
    private final ActivityResultLauncher<Intent> launcher;
    /** Role to use for a brand-new account; null means ask the user with the green role card. */
    private String preferredRole;

    public GoogleAuth(AppCompatActivity act, AuthViewModel vm, View progress) {
        this.act = act;
        this.vm = vm;
        this.progress = progress;
        this.webClientId = act.getString(R.string.google_web_client_id);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId)
                .build();
        client = GoogleSignIn.getClient(act, gso);

        launcher = act.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
            try {
                GoogleSignInAccount acct = GoogleSignIn.getSignedInAccountFromIntent(res.getData()).getResult(ApiException.class);
                String idToken = acct.getIdToken();
                if (idToken == null) { Ui.toast(act, I18n.t("Google sign-in failed")); return; }
                login(idToken, preferredRole);
            } catch (ApiException e) {
                if (e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED)
                    Ui.toast(act, I18n.t("Google sign-in failed") + " (" + e.getStatusCode() + ")");
            }
        });
    }

    /**
     * @param role "BUYER" or "FARMER" when the screen already knows it (the Register screen's choice);
     *             null to ask only if the Google account turns out to be new.
     */
    public void start(String role) {
        if (webClientId.startsWith("YOUR_")) { Ui.toast(act, I18n.t("Google sign-in is not set up yet")); return; }
        preferredRole = role;
        // sign out first so the account picker always shows
        client.signOut().addOnCompleteListener(t -> launcher.launch(client.getSignInIntent()));
    }

    /** role == null for a first try; if the server says NEW_ACCOUNT we ask Buyer/Farmer and call again with the same token. */
    private void login(String idToken, String role) {
        vm.google(idToken, role).observe(act, r -> {
            if (progress != null) progress.setVisibility(r.status == Resource.Status.LOADING ? View.VISIBLE : View.GONE);
            if (r.status == Resource.Status.SUCCESS) {
                Session.save(r.data);
                Ui.home(act);
            } else if (r.status == Resource.Status.ERROR) {
                if ("NEW_ACCOUNT".equals(r.message)) askRole(idToken);
                else Ui.toast(act, r.message);
            }
        });
    }

    /** New Google account: pick Buyer or Farmer on a branded green card instead of the stock list dialog. */
    private void askRole(String idToken) {
        View content = act.getLayoutInflater().inflate(R.layout.dialog_google_role, null);
        ((TextView) content.findViewById(R.id.tvRoleTitle)).setText(I18n.t("Create your account as"));

        AlertDialog dialog = new AlertDialog.Builder(act).setView(content).create();
        content.findViewById(R.id.optBuyer).setOnClickListener(v -> { dialog.dismiss(); login(idToken, "BUYER"); });
        content.findViewById(R.id.optFarmer).setOnClickListener(v -> { dialog.dismiss(); login(idToken, "FARMER"); });
        content.findViewById(R.id.tvRoleCancel).setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            // transparent window + side inset so only our rounded white card shows
            int inset = (int) (20 * act.getResources().getDisplayMetrics().density);
            dialog.getWindow().setBackgroundDrawable(new InsetDrawable(new ColorDrawable(Color.TRANSPARENT), inset));
        }
        dialog.show();
    }
}

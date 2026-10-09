package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityLoginBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Session.isLoggedIn()) { Ui.home(this); return; }
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);

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
    }
}

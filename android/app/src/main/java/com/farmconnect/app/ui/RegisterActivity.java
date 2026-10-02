package com.farmconnect.app.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.databinding.ActivityRegisterBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        ActivityRegisterBinding b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);

        b.btnRegister.setOnClickListener(v -> {
            String name = b.etName.getText().toString().trim();
            String email = b.etEmail.getText().toString().trim();
            String phone = b.etPhone.getText().toString().trim();
            String pass = b.etPassword.getText().toString();
            String role = b.rbFarmer.isChecked() ? "FARMER" : "BUYER";
            if (name.isEmpty() || email.isEmpty() || pass.length() < 6) {
                Ui.toast(this, "Fill all fields (password min 6 characters)");
                return;
            }
            Ui.watch(this, vm.register(name, email, phone, pass, role), b.progress, r -> {
                Session.save(r);
                Ui.home(this);
            });
        });
        b.tvLogin.setOnClickListener(v -> finish());
    }
}
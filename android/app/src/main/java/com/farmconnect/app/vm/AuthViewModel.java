package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;

public class AuthViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<AuthResponse>> login(String email, String pass) { return repo.login(email, pass); }
    public LiveData<Resource<AuthResponse>> register(String name, String email, String phone, String pass, String role) {
        return repo.register(new RegisterRequest(name, email, phone, pass, role));
    }
}

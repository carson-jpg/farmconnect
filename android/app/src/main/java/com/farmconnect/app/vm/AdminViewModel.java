package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

public class AdminViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<AdminStats>> stats() { return repo.adminStats(); }
    public LiveData<Resource<List<UserSummary>>> users(String role, String q) { return repo.adminUsers(role, q); }
    public LiveData<Resource<UserSummary>> setUserEnabled(long id, boolean enabled) { return repo.setUserEnabled(id, enabled); }
    public LiveData<Resource<List<Order>>> orders() { return repo.adminOrders(); }
    public LiveData<Resource<Order>> setOrderStatus(long id, String status) { return repo.adminSetOrderStatus(id, status); }
    public LiveData<Resource<List<Product>>> products(String q) { return repo.adminProducts(q); }
    public LiveData<Resource<Product>> setProductActive(long id, boolean active) { return repo.adminSetProductActive(id, active); }
    public LiveData<Resource<AuthResponse>> createOfficer(OfficerRequest r) { return repo.createOfficer(r); }
}

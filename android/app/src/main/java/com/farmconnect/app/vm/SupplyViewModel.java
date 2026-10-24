package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

/** Supplier listings and orders: the company side and the farmer side. */
public class SupplyViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<List<SupplyProduct>>> companyProducts() { return repo.companyProducts(); }
    public LiveData<Resource<SupplyProduct>> saveProduct(long id, SupplyProductRequest r) { return repo.saveSupplyProduct(id, r); }
    public LiveData<Resource<List<SupplyOrder>>> companyOrders(String status) { return repo.companySupplyOrders(status == null || status.isEmpty() ? null : status); }
    public LiveData<Resource<SupplyOrder>> step(String step, long id) { return repo.supplyStep(step, id); }
    public LiveData<Resource<SupplyOrder>> reject(long id, String reason) { return repo.supplyReject(id, reason); }
    public LiveData<Resource<SupplySummary>> summary() { return repo.companySummary(); }

    public LiveData<Resource<List<SupplyProduct>>> browse(String category, String q) {
        return repo.browseSupplies(category == null || category.isEmpty() ? null : category, q == null || q.isEmpty() ? null : q);
    }
    public LiveData<Resource<SupplyOrder>> place(PlaceSupplyOrder r) { return repo.placeSupplyOrder(r); }
    public LiveData<Resource<List<SupplyOrder>>> myOrders() { return repo.mySupplyOrders(); }
    public LiveData<Resource<SupplyOrder>> cancel(long id) { return repo.cancelSupplyOrder(id); }

    public LiveData<Resource<SupplyPaymentView>> pay(long orderId, String phone) { return repo.paySupplyOrder(orderId, phone); }
    public LiveData<Resource<FlagResponse>> paymentsEnabled() { return repo.supplyPaymentsEnabled(); }
    public LiveData<Resource<List<PayoutView>>> payouts() { return repo.companyPayouts(); }
}

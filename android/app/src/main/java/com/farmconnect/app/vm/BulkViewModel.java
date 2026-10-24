package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

/** Bulk produce requests, quotations and purchase orders: the company side and the farmer side. */
public class BulkViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<List<BulkRequest>>> companyRequests() { return repo.companyRequests(); }
    public LiveData<Resource<BulkRequest>> create(BulkRequestForm f) { return repo.createBulkRequest(f); }
    public LiveData<Resource<BulkRequest>> close(long id) { return repo.closeBulkRequest(id); }
    public LiveData<Resource<List<BulkQuote>>> quotations(long requestId) { return repo.requestQuotations(requestId); }
    public LiveData<Resource<BulkOrder>> accept(long quoteId) { return repo.acceptQuotation(quoteId); }
    public LiveData<Resource<BulkQuote>> rejectQuote(long quoteId, String reason) { return repo.rejectQuotation(quoteId, reason); }
    public LiveData<Resource<List<BulkOrder>>> companyOrders() { return repo.companyPurchaseOrders(); }
    public LiveData<Resource<BulkOrder>> step(String step, long id) { return repo.poStep(step, id); }
    public LiveData<Resource<BulkOrder>> cancelOrder(long id, String reason) { return repo.poCancel(id, reason); }

    public LiveData<Resource<List<BulkRequest>>> browse(String produce) { return repo.browseBulk(produce == null || produce.isEmpty() ? null : produce, null); }
    public LiveData<Resource<BulkQuote>> sendQuote(long requestId, BulkQuoteForm f) { return repo.sendQuote(requestId, f); }
    public LiveData<Resource<List<BulkQuote>>> myQuotes() { return repo.myQuotes(); }
    public LiveData<Resource<BulkQuote>> withdraw(long id) { return repo.withdrawQuote(id); }
    public LiveData<Resource<List<BulkOrder>>> myOrders() { return repo.myPurchaseOrders(); }
    public LiveData<Resource<BulkOrder>> confirm(long id) { return repo.poConfirm(id); }
    public LiveData<Resource<BulkOrder>> decline(long id, String reason) { return repo.poDecline(id, reason); }
}

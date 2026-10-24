package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

/** Team, services companies offer, invoices, and the super admin's marketplace overview. */
public class MarketViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    private static String none(String s) { return s == null || s.isEmpty() ? null : s; }

    public LiveData<Resource<List<TeamMember>>> team() { return repo.companyTeam(); }
    public LiveData<Resource<TeamMember>> saveMember(long id, TeamMemberForm f) { return repo.saveTeamMember(id, f); }
    public LiveData<Resource<Void>> deleteMember(long id) { return repo.deleteTeamMember(id); }

    public LiveData<Resource<List<HireService>>> services() { return repo.companyServices(); }
    public LiveData<Resource<HireService>> saveService(long id, HireServiceForm f) { return repo.saveHireService(id, f); }
    public LiveData<Resource<HireService>> hideService(long id) { return repo.hideHireService(id); }
    public LiveData<Resource<List<HireRequest>>> serviceRequests(String status) { return repo.companyServiceRequests(none(status)); }
    public LiveData<Resource<HireRequest>> accept(long id, HireAccept f) { return repo.acceptServiceRequest(id, f); }
    public LiveData<Resource<HireRequest>> decline(long id, String reason) { return repo.declineServiceRequest(id, reason); }
    public LiveData<Resource<HireRequest>> complete(long id) { return repo.completeServiceRequest(id); }

    public LiveData<Resource<Invoice>> invoice(long orderId) { return repo.supplyInvoice(orderId); }
    public LiveData<Resource<BulkRequest>> updateRequest(long id, BulkRequestForm f) { return repo.updateBulkRequest(id, f); }

    public LiveData<Resource<List<HireService>>> browse(String category, String q) { return repo.browseHire(none(category), none(q)); }
    public LiveData<Resource<HireRequest>> request(long serviceId, HireRequestForm f) { return repo.requestHire(serviceId, f); }
    public LiveData<Resource<List<HireRequest>>> myRequests() { return repo.myHireRequests(); }
    public LiveData<Resource<HireRequest>> cancel(long id) { return repo.cancelHireRequest(id); }

    public LiveData<Resource<MarketOverview>> overview() { return repo.marketOverview(); }
    public LiveData<Resource<List<SupplyOrder>>> adminSupplyOrders() { return repo.adminSupplyOrders(null); }
    public LiveData<Resource<List<BulkRequest>>> adminProduceRequests() { return repo.adminProduceRequests(null); }
    public LiveData<Resource<List<BulkOrder>>> adminPurchaseOrders() { return repo.adminPurchaseOrders(null); }
    public LiveData<Resource<List<HireRequest>>> adminServiceRequests() { return repo.adminServiceRequests(null); }
}

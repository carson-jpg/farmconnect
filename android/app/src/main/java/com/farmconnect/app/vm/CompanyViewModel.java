package com.farmconnect.app.vm;

import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

/** Company portal (the company's own side) and company verification (super admin). */
public class CompanyViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<CompanyView>> mine() { return repo.myCompany(); }
    public LiveData<Resource<CompanyView>> register(CompanyRequest r) { return repo.registerCompany(r); }
    public LiveData<Resource<CompanyView>> update(CompanyRequest r) { return repo.updateCompany(r); }
    public LiveData<Resource<CompanyView>> uploadDoc(String type, byte[] data, String mime, String name) { return repo.uploadCompanyDoc(type, data, mime, name); }
    public LiveData<Resource<CompanyView>> deleteDoc(long id) { return repo.deleteCompanyDoc(id); }
    public LiveData<Resource<Bitmap>> doc(boolean admin, long companyId, long docId) { return repo.companyDoc(admin, companyId, docId); }

    public LiveData<Resource<List<AdminCompanySummary>>> adminList(String status) { return repo.adminCompanies(status); }
    public LiveData<Resource<AdminCompanyDetail>> adminDetail(long id) { return repo.adminCompany(id); }
    public LiveData<Resource<AdminCompanyDetail>> startReview(long id) { return repo.companyStartReview(id); }
    public LiveData<Resource<AdminCompanyDetail>> verify(long id) { return repo.companyVerify(id); }
    public LiveData<Resource<AdminCompanyDetail>> reject(long id, String reason) { return repo.companyReject(id, reason); }
    public LiveData<Resource<AdminCompanyDetail>> suspend(long id, String reason) { return repo.companySuspend(id, reason); }
    public LiveData<Resource<AdminCompanyDetail>> reinstate(long id) { return repo.companyReinstate(id); }

    public LiveData<Resource<CompanyView>> setPayoutAccount(PayoutAccountRequest r) { return repo.setPayoutAccount(r); }
    public LiveData<Resource<AdminCompanyDetail>> approvePayout(long id) { return repo.companyApprovePayout(id); }
    public LiveData<Resource<AdminCompanyDetail>> revokePayout(long id) { return repo.companyRevokePayout(id); }
}

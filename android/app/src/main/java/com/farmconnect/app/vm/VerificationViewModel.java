package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;

public class VerificationViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<VerificationResponse>> mine() { return repo.myVerification(); }
    public LiveData<Resource<VerificationResponse>> saveDraft(DraftRequest r) { return repo.saveDraft(r); }
    public LiveData<Resource<Void>> sendOtp(String phone) { return repo.sendOtp(phone); }
    public LiveData<Resource<VerificationResponse>> verifyOtp(String phone, String code) { return repo.verifyOtp(phone, code); }
    public LiveData<Resource<VerificationResponse>> uploadDoc(String type, byte[] jpeg) { return repo.uploadDoc(type, jpeg); }
    public LiveData<Resource<VerificationResponse>> submit() { return repo.submitVerification(); }
}
package com.farmconnect.app.vm;

import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

public class ReviewViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<List<ReviewSummary>>> list(String status) { return repo.reviews(status); }
    public LiveData<Resource<ReviewDetail>> detail(long id) { return repo.reviewDetail(id); }
    public LiveData<Resource<ReviewDetail>> start(long id) { return repo.startReview(id); }
    public LiveData<Resource<ReviewDetail>> approve(long id) { return repo.approve(id); }
    public LiveData<Resource<ReviewDetail>> reject(long id, String reason) { return repo.reject(id, reason); }
    public LiveData<Resource<Bitmap>> doc(long id, String type) { return repo.reviewDoc(id, type); }
}
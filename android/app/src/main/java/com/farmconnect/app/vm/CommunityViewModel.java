package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

/** One view model for information, messages, notifications, weather, records, directory, groups, feedback and analytics. */
public class CommunityViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<List<Post>>> posts(String type, String q) { return repo.posts(type, q); }
    public LiveData<Resource<Post>> post(long id) { return repo.post(id); }
    public LiveData<Resource<Post>> createPost(PostRequest r) { return repo.createPost(r); }
    public LiveData<Resource<Void>> deletePost(long id) { return repo.deletePost(id); }
    public LiveData<Resource<Post>> togglePostRegistration(long id) { return repo.togglePostRegistration(id); }
    public LiveData<Resource<List<Conversation>>> conversations() { return repo.conversations(); }
    public LiveData<Resource<List<ChatMessage>>> thread(long userId) { return repo.thread(userId); }
    public LiveData<Resource<ChatMessage>> sendMessage(long to, String body) { return repo.sendMessage(to, body); }
    public LiveData<Resource<List<Contact>>> contacts(String q) { return repo.contacts(q); }
    public LiveData<Resource<List<AppNotification>>> notifications() { return repo.notifications(); }
    public LiveData<Resource<Counts>> counts() { return repo.counts(); }
    public LiveData<Resource<Counts>> readAllNotifications() { return repo.readAllNotifications(); }
    public LiveData<Resource<Counts>> readNotification(long id) { return repo.readNotification(id); }
    public LiveData<Resource<Weather>> weather() { return repo.weather(); }
    public LiveData<Resource<List<FarmRecord>>> records(String type) { return repo.records(type); }
    public LiveData<Resource<FarmRecord>> createRecord(RecordRequest r) { return repo.createRecord(r); }
    public LiveData<Resource<FarmRecord>> updateRecord(long id, RecordRequest r) { return repo.updateRecord(id, r); }
    public LiveData<Resource<Void>> deleteRecord(long id) { return repo.deleteRecord(id); }
    public LiveData<Resource<RecordSummary>> recordSummary() { return repo.recordSummary(); }
    public LiveData<Resource<List<ServiceProvider>>> services(String category, String q) { return repo.services(category, q); }
    public LiveData<Resource<ServiceProvider>> createService(ServiceRequest r) { return repo.createService(r); }
    public LiveData<Resource<Void>> deleteService(long id) { return repo.deleteService(id); }
    public LiveData<Resource<List<Group>>> groups(boolean mine, String type, String q) { return repo.groups(mine, type, q); }
    public LiveData<Resource<GroupDetail>> group(long id) { return repo.group(id); }
    public LiveData<Resource<Group>> createGroup(GroupRequest r) { return repo.createGroup(r); }
    public LiveData<Resource<Void>> deleteGroup(long id) { return repo.deleteGroup(id); }
    public LiveData<Resource<Group>> joinGroup(long id) { return repo.joinGroup(id); }
    public LiveData<Resource<Group>> leaveGroup(long id) { return repo.leaveGroup(id); }
    public LiveData<Resource<Void>> removeGroupMember(long id, long userId) { return repo.removeGroupMember(id, userId); }
    public LiveData<Resource<Void>> announceToGroup(long id, String title, String body) { return repo.announceToGroup(id, title, body); }
    public LiveData<Resource<Void>> sendFeedback(int rating, String comment) { return repo.sendFeedback(rating, comment); }
    public LiveData<Resource<Analytics>> analytics() { return repo.analytics(); }
    public LiveData<Resource<List<Price>>> prices(String q) { return repo.prices(q); }
    public LiveData<Resource<List<PricePoint>>> priceHistory(String crop, String market) { return repo.priceHistory(crop, market); }
    public LiveData<Resource<Price>> postPrice(PriceRequest r) { return repo.postPrice(r); }
    public LiveData<Resource<Void>> deletePrice(long id) { return repo.deletePrice(id); }
    public LiveData<Resource<AlertToggle>> togglePriceAlert(String crop) { return repo.togglePriceAlert(crop); }
    public LiveData<Resource<Review>> createReview(long orderId, long sellerId, int rating, String comment) { return repo.createReview(orderId, sellerId, rating, comment); }
    public LiveData<Resource<SellerReviewSummary>> sellerReviews(long id) { return repo.sellerReviews(id); }
    public LiveData<Resource<Review>> replyReview(long id, String text) { return repo.replyReview(id, text); }
    public LiveData<Resource<Void>> deleteReview(long id) { return repo.deleteReview(id); }
    public LiveData<Resource<List<Farm>>> myFarms() { return repo.myFarms(); }
}

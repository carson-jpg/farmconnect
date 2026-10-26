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
    public LiveData<Resource<AlertInfo>> setPriceTarget(String crop, Double price, String direction) { return repo.setPriceTarget(crop, price, direction); }
    public LiveData<Resource<MessageResponse>> emailReport() { return repo.emailReport(); }
    public LiveData<Resource<TrendResponse>> priceTrend(String crop, String market, int days) { return repo.priceTrend(crop, market, days); }
    public LiveData<Resource<CalendarResponse>> calendar() { return repo.calendar(); }
    public LiveData<Resource<AuthResponse>> changePassword(String current, String next) { return repo.changePassword(current, next); }
    public LiveData<Resource<SecuritySummary>> securitySummary() { return repo.securitySummary(); }
    public LiveData<Resource<List<AuditEntry>>> securityAudit(String action, String q) { return repo.securityAudit(action, q); }
    public LiveData<Resource<List<LockInfo>>> securityLocks() { return repo.securityLocks(); }
    public LiveData<Resource<Cleared>> unlock(String email, String ip) { return repo.unlock(email, ip); }
    public LiveData<Resource<MessageResponse>> forceLogout(long id) { return repo.forceLogout(id); }
    public LiveData<Resource<TempPassword>> adminResetPassword(long id) { return repo.adminResetPassword(id); }
    public LiveData<Resource<List<UserSummary>>> countyAdmins() { return repo.countyAdmins(); }
    public LiveData<Resource<UserSummary>> createCountyAdmin(CountyAdminRequest r) { return repo.createCountyAdmin(r); }
    public LiveData<Resource<UserSummary>> setCountyAdminCounty(long id, String county) { return repo.setCountyAdminCounty(id, county); }
    public LiveData<Resource<Review>> createReview(long orderId, long sellerId, int rating, String comment) { return repo.createReview(orderId, sellerId, rating, comment); }
    public LiveData<Resource<SellerReviewSummary>> sellerReviews(long id) { return repo.sellerReviews(id); }
    public LiveData<Resource<Review>> replyReview(long id, String text) { return repo.replyReview(id, text); }
    public LiveData<Resource<Void>> deleteReview(long id) { return repo.deleteReview(id); }
    public LiveData<Resource<List<Farm>>> myFarms() { return repo.myFarms(); }
    public LiveData<Resource<CropGuide>> cropGuide() { return repo.cropGuide(); }
    public LiveData<Resource<CropDiagnosis>> cropDiagnose(String crop, List<String> symptoms) { return repo.cropDiagnose(crop, symptoms); }
    public LiveData<Resource<CropCase>> cropSend(String crop, List<String> symptoms, String description) { return repo.cropSend(crop, symptoms, description); }
    public LiveData<Resource<CropCase>> cropPhoto(long id, byte[] jpeg) { return repo.cropPhoto(id, jpeg); }
    public LiveData<Resource<List<CropCase>>> cropCases(String status) { return repo.cropCases(status); }
    public LiveData<Resource<CropCase>> cropCase(long id) { return repo.cropCase(id); }
    public LiveData<Resource<CropCase>> cropReview(long id, String note, String confirmed) { return repo.cropReview(id, note, confirmed); }
    public LiveData<Resource<CropCase>> cropClose(long id) { return repo.cropClose(id); }
    public LiveData<Resource<List<CashSale>>> cashMine() { return repo.cashMine(); }
    public LiveData<Resource<List<CashSale>>> cashForOrder(long id) { return repo.cashForOrder(id); }
    public LiveData<Resource<CashSale>> cashRecord(long orderId, String note) { return repo.cashRecord(orderId, note); }
    public LiveData<Resource<CashSale>> cashConfirm(long id) { return repo.cashConfirm(id); }
    public LiveData<Resource<CashSale>> cashDispute(long id, String note) { return repo.cashDispute(id, note); }
    public LiveData<Resource<List<CashBill>>> cashBills() { return repo.cashBills(); }
    public LiveData<Resource<CashBill>> cashPay(long id, String phone) { return repo.cashPay(id, phone); }
    public LiveData<Resource<List<Deal>>> deals(Long groupId, String kind) { return repo.deals(groupId, kind); }
    public LiveData<Resource<List<Deal>>> dealMarket() { return repo.dealMarket(); }
    public LiveData<Resource<DealDetail>> deal(long id) { return repo.deal(id); }
    public LiveData<Resource<DealDetail>> dealCreate(DealRequest r) { return repo.dealCreate(r); }
    public LiveData<Resource<DealDetail>> dealShare(long id, double qty) { return repo.dealShare(id, qty); }
    public LiveData<Resource<DealDetail>> dealClose(long id) { return repo.dealClose(id); }
    public LiveData<Resource<DealDetail>> dealComplete(long id, Double price, String note) { return repo.dealComplete(id, price, note); }
    public LiveData<Resource<DealDetail>> dealCancel(long id) { return repo.dealCancel(id); }
    public LiveData<Resource<DeliveryRates>> deliveryRates() { return repo.deliveryRates(); }
    public LiveData<Resource<DeliveryRates>> setDeliveryRates(double base, double perKm, Double factor, Double maxKm) { return repo.setDeliveryRates(base, perKm, factor, maxKm); }
    public LiveData<Resource<DeliveryOptions>> deliveryOptions(long orderId, Double lat, Double lng) { return repo.deliveryOptions(orderId, lat, lng); }
    public LiveData<Resource<DeliveryJob>> deliveryRequest(long orderId, long transporterId, Double lat, Double lng, String note) { return repo.deliveryRequest(orderId, transporterId, lat, lng, note); }
    public LiveData<Resource<List<DeliveryJob>>> deliveryJobs(String status) { return repo.deliveryJobs(status); }
    public LiveData<Resource<DeliveryJob>> deliveryCancel(long id) { return repo.deliveryCancel(id); }
    public LiveData<Resource<DeliveryJob>> deliveryAccept(long id, Double km) { return repo.deliveryAccept(id, km); }
    public LiveData<Resource<DeliveryJob>> deliveryDecline(long id, String reason) { return repo.deliveryDecline(id, reason); }
    public LiveData<Resource<DeliveryJob>> deliveryPickup(long id) { return repo.deliveryPickup(id); }
    public LiveData<Resource<DeliveryJob>> deliveryDeliver(long id) { return repo.deliveryDeliver(id); }
    public LiveData<Resource<DeliveryJob>> deliveryFeeReceived(long id) { return repo.deliveryFeeReceived(id); }
}

package com.farmconnect.app.data;

import com.farmconnect.app.util.I18n;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.farmconnect.app.data.Models.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Repository {
    private static final Repository I = new Repository();
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    public static Repository get() { return I; }
    private ApiService api() { return ApiClient.get(); }

    // auth
    public LiveData<Resource<AuthResponse>> login(String e, String p) { return run(api().login(new LoginRequest(e, p))); }
    public LiveData<Resource<AuthResponse>> register(RegisterRequest r) { return run(api().register(r)); }
    public LiveData<Resource<AuthResponse>> google(String idToken, String role) { return run(api().google(new GoogleRequest(idToken, role))); }
    public LiveData<Resource<MessageResponse>> forgotPassword(String email) { return run(api().forgotPassword(new ForgotRequest(email))); }
    public LiveData<Resource<MessageResponse>> resetPassword(String email, String code, String newPassword) {
        return run(api().resetPassword(new ResetRequest(email, code, newPassword)));
    }
    // farms
    public LiveData<Resource<List<Farm>>> myFarms() { return run(api().myFarms()); }
    public LiveData<Resource<Farm>> createFarm(FarmRequest r) { return run(api().createFarm(r)); }
    public LiveData<Resource<Farm>> updateFarm(long id, FarmRequest r) { return run(api().updateFarm(id, r)); }
    public LiveData<Resource<Void>> deleteFarm(long id) { return run(api().deleteFarm(id)); }
    // products
    public LiveData<Resource<List<Product>>> products(String q) { return run(api().products(q == null || q.isEmpty() ? null : q)); }
    public LiveData<Resource<List<Product>>> myProducts() { return run(api().myProducts()); }
    public LiveData<Resource<Product>> product(long id) { return run(api().product(id)); }
    public LiveData<Resource<Product>> addProductImage(long id, byte[] jpeg) {
        RequestBody body = RequestBody.create(jpeg, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "photo.jpg", body);
        return run(api().addProductImage(id, part));
    }
    public LiveData<Resource<Product>> deleteProductImage(long id, int index) { return run(api().deleteProductImage(id, index)); }
    public LiveData<Resource<Product>> createProduct(ProductRequest r) { return run(api().createProduct(r)); }
    public LiveData<Resource<Product>> updateProduct(long id, ProductRequest r) { return run(api().updateProduct(id, r)); }
    public LiveData<Resource<Void>> deleteProduct(long id) { return run(api().deleteProduct(id)); }
    // cart
    public LiveData<Resource<List<CartItem>>> cart() { return run(api().cart()); }
    public LiveData<Resource<CartItem>> addToCart(long pid, int q) { return run(api().addToCart(new CartRequest(pid, q))); }
    public LiveData<Resource<CartItem>> setQty(long id, int q) { return run(api().setQty(id, new QuantityRequest(q))); }
    public LiveData<Resource<Void>> removeCartItem(long id) { return run(api().removeCartItem(id)); }
    // wishlist
    public LiveData<Resource<List<Product>>> wishlist() { return run(api().wishlist()); }
    public LiveData<Resource<Void>> addWish(long pid) { return run(api().addWish(pid)); }
    public LiveData<Resource<Void>> removeWish(long pid) { return run(api().removeWish(pid)); }
    // orders
    public LiveData<Resource<Order>> placeOrder(String address) { return run(api().placeOrder(new OrderRequest(address))); }
    public LiveData<Resource<Order>> placeOrder(OrderRequest r) { return run(api().placeOrder(r)); }
    public LiveData<Resource<List<Order>>> myOrders() { return run(api().myOrders()); }
    public LiveData<Resource<PayStatus>> startStk(long orderId, String phone) { return run(api().startStk(orderId, new PhoneRequest(phone))); }
    public LiveData<Resource<PayStatus>> payStatus(long orderId) { return run(api().payStatus(orderId)); }
    public LiveData<Resource<Earnings>> earnings() { return run(api().earnings()); }
    public LiveData<Resource<PayoutAccount>> payoutAccount() { return run(api().payoutAccount()); }
    public LiveData<Resource<PayoutAccount>> setPayoutPhone(String phone) { return run(api().setPayoutAccount(new PhoneRequest(phone))); }
    public LiveData<Resource<Order>> cancelOrder(long id) { return run(api().cancelOrder(id)); }
    public LiveData<Resource<List<Order>>> farmerOrders() { return run(api().farmerOrders()); }
    public LiveData<Resource<Order>> updateStatus(long id, String s) { return run(api().updateStatus(id, new StatusRequest(s))); }

    // farmer verification
    public LiveData<Resource<VerificationResponse>> myVerification() { return run(api().myVerification()); }
    public LiveData<Resource<VerificationResponse>> saveDraft(DraftRequest r) { return run(api().saveDraft(r)); }
    public LiveData<Resource<Void>> sendOtp(String phone) { return run(api().sendOtp(new PhoneRequest(phone))); }
    public LiveData<Resource<VerificationResponse>> verifyOtp(String phone, String code) {
        return run(api().verifyOtp(new OtpVerifyRequest(phone, code)));
    }
    public LiveData<Resource<VerificationResponse>> submitVerification() { return run(api().submitVerification()); }
    public LiveData<Resource<VerificationResponse>> uploadDoc(String type, byte[] jpeg) {
        RequestBody t = RequestBody.create(type, MediaType.parse("text/plain"));
        RequestBody body = RequestBody.create(jpeg, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", type.toLowerCase() + ".jpg", body);
        return run(api().uploadDoc(t, part));
    }

    // community
    public LiveData<Resource<List<Post>>> posts(String type, String q) { return run(api().posts(emptyToNull(type), emptyToNull(q))); }
    public LiveData<Resource<Post>> post(long id) { return run(api().post(id)); }
    public LiveData<Resource<Post>> createPost(PostRequest r) { return run(api().createPost(r)); }
    public LiveData<Resource<Void>> deletePost(long id) { return run(api().deletePost(id)); }
    public LiveData<Resource<Post>> togglePostRegistration(long id) { return run(api().togglePostRegistration(id)); }
    public LiveData<Resource<List<Conversation>>> conversations() { return run(api().conversations()); }
    public LiveData<Resource<List<ChatMessage>>> thread(long userId) { return run(api().thread(userId)); }
    public LiveData<Resource<ChatMessage>> sendMessage(long to, String body) { return run(api().sendMessage(new ChatRequest(to, body))); }
    public LiveData<Resource<List<Contact>>> contacts(String q) { return run(api().contacts(emptyToNull(q))); }
    public LiveData<Resource<List<AppNotification>>> notifications() { return run(api().notifications()); }
    public LiveData<Resource<Counts>> counts() { return run(api().counts()); }
    public LiveData<Resource<Counts>> readAllNotifications() { return run(api().readAllNotifications()); }
    public LiveData<Resource<Counts>> readNotification(long id) { return run(api().readNotification(id)); }
    public LiveData<Resource<Weather>> weather() { return run(api().weather()); }
    public LiveData<Resource<List<FarmRecord>>> records(String type) { return run(api().records(emptyToNull(type))); }
    public LiveData<Resource<FarmRecord>> createRecord(RecordRequest r) { return run(api().createRecord(r)); }
    public LiveData<Resource<FarmRecord>> updateRecord(long id, RecordRequest r) { return run(api().updateRecord(id, r)); }
    public LiveData<Resource<Void>> deleteRecord(long id) { return run(api().deleteRecord(id)); }
    public LiveData<Resource<RecordSummary>> recordSummary() { return run(api().recordSummary()); }
    public LiveData<Resource<List<ServiceProvider>>> services(String category, String q) { return run(api().services(emptyToNull(category), emptyToNull(q))); }
    public LiveData<Resource<ServiceProvider>> createService(ServiceRequest r) { return run(api().createService(r)); }
    public LiveData<Resource<Void>> deleteService(long id) { return run(api().deleteService(id)); }
    public LiveData<Resource<List<Group>>> groups(boolean mine, String type, String q) { return run(api().groups(mine ? Boolean.TRUE : null, emptyToNull(type), emptyToNull(q))); }
    public LiveData<Resource<GroupDetail>> group(long id) { return run(api().group(id)); }
    public LiveData<Resource<Group>> createGroup(GroupRequest r) { return run(api().createGroup(r)); }
    public LiveData<Resource<Void>> deleteGroup(long id) { return run(api().deleteGroup(id)); }
    public LiveData<Resource<Group>> joinGroup(long id) { return run(api().joinGroup(id)); }
    public LiveData<Resource<Group>> leaveGroup(long id) { return run(api().leaveGroup(id)); }
    public LiveData<Resource<Void>> removeGroupMember(long id, long userId) { return run(api().removeGroupMember(id, userId)); }
    public LiveData<Resource<Void>> announceToGroup(long id, String title, String body) { return run(api().announceToGroup(id, new AnnounceRequest(title, body))); }
    public LiveData<Resource<Void>> sendFeedback(int rating, String comment) { return run(api().sendFeedback(new FeedbackRequest(rating, comment))); }
    public LiveData<Resource<Analytics>> analytics() { return run(api().analytics()); }

    public LiveData<Resource<List<Price>>> prices(String q) { return run(api().prices(emptyToNull(q))); }
    public LiveData<Resource<List<PricePoint>>> priceHistory(String crop, String market) { return run(api().priceHistory(crop, market)); }
    public LiveData<Resource<Price>> postPrice(PriceRequest r) { return run(api().postPrice(r)); }
    public LiveData<Resource<Void>> deletePrice(long id) { return run(api().deletePrice(id)); }
    public LiveData<Resource<AlertToggle>> togglePriceAlert(String crop) { return run(api().togglePriceAlert(crop)); }
    public LiveData<Resource<AlertInfo>> setPriceTarget(String crop, Double price, String direction) { return run(api().setPriceTarget(crop, price, direction)); }
    public LiveData<Resource<Review>> createReview(long orderId, long sellerId, int rating, String comment) {
        return run(api().createReview(new ReviewRequest(orderId, sellerId, rating, comment)));
    }
    public LiveData<Resource<SellerReviewSummary>> sellerReviews(long id) { return run(api().sellerReviews(id)); }
    public LiveData<Resource<Review>> replyReview(long id, String text) { return run(api().replyReview(id, new ReplyRequest(text))); }
    public LiveData<Resource<Void>> deleteReview(long id) { return run(api().deleteReview(id)); }

    private static String emptyToNull(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }

    // admin
    public LiveData<Resource<AdminStats>> adminStats() { return run(api().adminStats()); }
    public LiveData<Resource<List<UserSummary>>> adminUsers(String role, String q) {
        return run(api().adminUsers(role == null || role.isEmpty() ? null : role, q == null || q.isEmpty() ? null : q));
    }
    public LiveData<Resource<UserSummary>> setUserEnabled(long id, boolean enabled) { return run(api().setUserEnabled(id, new ActiveRequest(enabled))); }
    public LiveData<Resource<List<Order>>> adminOrders() { return run(api().adminOrders()); }
    public LiveData<Resource<Order>> adminSetOrderStatus(long id, String s) { return run(api().adminSetOrderStatus(id, new StatusRequest(s))); }
    public LiveData<Resource<List<Product>>> adminProducts(String q) { return run(api().adminProducts(q == null || q.isEmpty() ? null : q)); }
    public LiveData<Resource<Product>> adminSetProductActive(long id, boolean active) { return run(api().adminSetProductActive(id, new ActiveRequest(active))); }
    public LiveData<Resource<AuthResponse>> createOfficer(OfficerRequest r) { return run(api().createOfficer(r)); }
    public LiveData<Resource<MessageResponse>> emailReport() { return run(api().emailReport()); }
    public LiveData<Resource<TrendResponse>> priceTrend(String crop, String market, int days) { return run(api().priceTrend(crop, market, days)); }
    public LiveData<Resource<CalendarResponse>> calendar() { return run(api().calendar()); }
    public LiveData<Resource<AuthResponse>> changePassword(String current, String next) { return run(api().changePassword(new ChangePasswordRequest(current, next))); }
    public LiveData<Resource<SecuritySummary>> securitySummary() { return run(api().securitySummary()); }
    public LiveData<Resource<List<AuditEntry>>> securityAudit(String action, String q) { return run(api().securityAudit(action, q, 200)); }
    public LiveData<Resource<List<LockInfo>>> securityLocks() { return run(api().securityLocks()); }
    public LiveData<Resource<Cleared>> unlock(String email, String ip) { return run(api().unlock(new UnlockRequest(email, ip))); }
    public LiveData<Resource<MessageResponse>> forceLogout(long id) { return run(api().forceLogout(id)); }
    public LiveData<Resource<TempPassword>> adminResetPassword(long id) { return run(api().adminResetPassword(id)); }
    public LiveData<Resource<List<UserSummary>>> countyAdmins() { return run(api().countyAdmins()); }
    public LiveData<Resource<UserSummary>> createCountyAdmin(CountyAdminRequest r) { return run(api().createCountyAdmin(r)); }
    public LiveData<Resource<UserSummary>> setCountyAdminCounty(long id, String county) { return run(api().setCountyAdminCounty(id, new CountyRequest(county))); }
    public LiveData<Resource<UserSummary>> assignOfficer(long id, AssignmentRequest r) { return run(api().assignOfficer(id, r)); }

    // reviewer
    public LiveData<Resource<List<ReviewSummary>>> reviews(String status) { return run(api().reviews(status)); }
    public LiveData<Resource<ReviewDetail>> reviewDetail(long id) { return run(api().reviewDetail(id)); }
    public LiveData<Resource<ReviewDetail>> startReview(long id) { return run(api().startReview(id)); }
    public LiveData<Resource<ReviewDetail>> approve(long id) { return run(api().approve(id)); }
    public LiveData<Resource<ReviewDetail>> reject(long id, String reason) { return run(api().reject(id, new RejectRequest(reason))); }

    public LiveData<Resource<Bitmap>> reviewDoc(long id, String type) {
        MutableLiveData<Resource<Bitmap>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        api().reviewDoc(id, type).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                if (!r.isSuccessful() || r.body() == null) { live.setValue(Resource.error(errorText(r))); return; }
                ResponseBody body = r.body();
                IO.execute(() -> {
                    Bitmap bm = BitmapFactory.decodeStream(body.byteStream());
                    if (bm != null) live.postValue(Resource.success(bm));
                    else live.postValue(Resource.error(I18n.t("Could not read image")));
                });
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) {
                live.setValue(Resource.error(Offline.isOnline() ? I18n.t("Network error: ") + t.getMessage() : I18n.t("No internet. Please try again when you are online.")));
            }
        });
        return live;
    }

    // companies
    public LiveData<Resource<CompanyView>> myCompany() { return run(api().myCompany()); }
    public LiveData<Resource<CompanyView>> registerCompany(CompanyRequest r) { return run(api().registerCompany(r)); }
    public LiveData<Resource<CompanyView>> updateCompany(CompanyRequest r) { return run(api().updateCompany(r)); }
    public LiveData<Resource<CompanyView>> uploadCompanyDoc(String type, byte[] data, String mime, String fileName) {
        RequestBody t = RequestBody.create(type, MediaType.parse("text/plain"));
        RequestBody body = RequestBody.create(data, MediaType.parse(mime));
        return run(api().uploadCompanyDoc(t, MultipartBody.Part.createFormData("file", fileName, body)));
    }
    public LiveData<Resource<CompanyView>> deleteCompanyDoc(long id) { return run(api().deleteCompanyDoc(id)); }
    public LiveData<Resource<List<AdminCompanySummary>>> adminCompanies(String status) { return run(api().adminCompanies(status)); }
    public LiveData<Resource<AdminCompanyDetail>> adminCompany(long id) { return run(api().adminCompany(id)); }
    public LiveData<Resource<AdminCompanyDetail>> companyStartReview(long id) { return run(api().companyStartReview(id)); }
    public LiveData<Resource<AdminCompanyDetail>> companyVerify(long id) { return run(api().companyVerify(id)); }
    public LiveData<Resource<AdminCompanyDetail>> companyReject(long id, String reason) { return run(api().companyReject(id, new ReasonRequest(reason))); }
    public LiveData<Resource<AdminCompanyDetail>> companySuspend(long id, String reason) { return run(api().companySuspend(id, new ReasonRequest(reason))); }
    public LiveData<Resource<AdminCompanyDetail>> companyReinstate(long id) { return run(api().companyReinstate(id)); }

    /** A company document as a picture. admin=true reads it through the super-admin endpoint for companyId. PDFs cannot be shown here. */
    public LiveData<Resource<Bitmap>> companyDoc(boolean admin, long companyId, long docId) {
        MutableLiveData<Resource<Bitmap>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        Call<ResponseBody> call = admin ? api().adminCompanyDoc(companyId, docId) : api().companyDocFile(docId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> c, Response<ResponseBody> r) {
                if (!r.isSuccessful() || r.body() == null) { live.setValue(Resource.error(errorText(r))); return; }
                ResponseBody body = r.body();
                IO.execute(() -> {
                    Bitmap bm = BitmapFactory.decodeStream(body.byteStream());
                    if (bm != null) live.postValue(Resource.success(bm));
                    else live.postValue(Resource.error(I18n.t("This file is not a picture (a PDF?). Open it on the web portal.")));
                });
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) {
                live.setValue(Resource.error(Offline.isOnline() ? I18n.t("Network error: ") + t.getMessage() : I18n.t("No internet. Please try again when you are online.")));
            }
        });
        return live;
    }

    // supplier listings and input orders
    public LiveData<Resource<List<SupplyProduct>>> companyProducts() { return run(api().companyProducts()); }
    public LiveData<Resource<SupplyProduct>> saveSupplyProduct(long id, SupplyProductRequest r) {
        return run(id == 0 ? api().createSupplyProduct(r) : api().updateSupplyProduct(id, r));
    }
    public LiveData<Resource<List<SupplyOrder>>> companySupplyOrders(String status) { return run(api().companySupplyOrders(status)); }
    public LiveData<Resource<SupplyOrder>> supplyStep(String step, long id) {
        switch (step) {
            case "confirm": return run(api().supplyConfirm(id));
            case "dispatch": return run(api().supplyDispatch(id));
            case "deliver": return run(api().supplyDeliver(id));
            default: return run(api().supplyMarkPaid(id));
        }
    }
    public LiveData<Resource<SupplyOrder>> supplyReject(long id, String reason) { return run(api().supplyReject(id, new ReasonRequest(reason))); }
    public LiveData<Resource<SupplySummary>> companySummary() { return run(api().companySummary()); }
    public LiveData<Resource<List<SupplyProduct>>> browseSupplies(String category, String q) { return run(api().browseSupplies(category, q)); }
    public LiveData<Resource<SupplyOrder>> placeSupplyOrder(PlaceSupplyOrder r) { return run(api().placeSupplyOrder(r)); }
    public LiveData<Resource<List<SupplyOrder>>> mySupplyOrders() { return run(api().mySupplyOrders()); }
    public LiveData<Resource<SupplyOrder>> cancelSupplyOrder(long id) { return run(api().cancelSupplyOrder(id)); }

    // supply payments and payouts
    public LiveData<Resource<CompanyView>> setPayoutAccount(PayoutAccountRequest r) { return run(api().setPayoutAccount(r)); }
    public LiveData<Resource<List<PayoutView>>> companyPayouts() { return run(api().companyPayouts()); }
    public LiveData<Resource<SupplyPaymentView>> paySupplyOrder(long id, String phone) { return run(api().paySupplyOrder(id, new PayRequest(phone))); }
    public LiveData<Resource<FlagResponse>> supplyPaymentsEnabled() { return run(api().supplyPaymentsEnabled()); }
    public LiveData<Resource<AdminCompanyDetail>> companyApprovePayout(long id) { return run(api().companyApprovePayout(id)); }
    public LiveData<Resource<AdminCompanyDetail>> companyRevokePayout(long id) { return run(api().companyRevokePayout(id)); }

    // bulk buying
    public LiveData<Resource<List<BulkRequest>>> companyRequests() { return run(api().companyRequests()); }
    public LiveData<Resource<BulkRequest>> createBulkRequest(BulkRequestForm f) { return run(api().createBulkRequest(f)); }
    public LiveData<Resource<BulkRequest>> closeBulkRequest(long id) { return run(api().closeBulkRequest(id)); }
    public LiveData<Resource<List<BulkQuote>>> requestQuotations(long id) { return run(api().requestQuotations(id)); }
    public LiveData<Resource<BulkOrder>> acceptQuotation(long id) { return run(api().acceptQuotation(id)); }
    public LiveData<Resource<BulkQuote>> rejectQuotation(long id, String reason) { return run(api().rejectQuotation(id, new ReasonRequest(reason))); }
    public LiveData<Resource<List<BulkOrder>>> companyPurchaseOrders() { return run(api().companyPurchaseOrders()); }
    public LiveData<Resource<BulkOrder>> poStep(String step, long id) {
        return "deliver".equals(step) ? run(api().poDeliver(id)) : run(api().poMarkPaid(id));
    }
    public LiveData<Resource<BulkOrder>> poCancel(long id, String reason) { return run(api().poCancel(id, new ReasonRequest(reason))); }
    public LiveData<Resource<List<BulkRequest>>> browseBulk(String produce, String county) { return run(api().browseBulk(produce, county)); }
    public LiveData<Resource<BulkQuote>> sendQuote(long id, BulkQuoteForm f) { return run(api().sendQuote(id, f)); }
    public LiveData<Resource<List<BulkQuote>>> myQuotes() { return run(api().myQuotes()); }
    public LiveData<Resource<BulkQuote>> withdrawQuote(long id) { return run(api().withdrawQuote(id)); }
    public LiveData<Resource<List<BulkOrder>>> myPurchaseOrders() { return run(api().myPurchaseOrders()); }
    public LiveData<Resource<BulkOrder>> poConfirm(long id) { return run(api().poConfirm(id)); }
    public LiveData<Resource<BulkOrder>> poDecline(long id, String reason) { return run(api().poDecline(id, new ReasonRequest(reason))); }

    // team, hire services, invoices, admin overview
    public LiveData<Resource<List<TeamMember>>> companyTeam() { return run(api().companyTeam()); }
    public LiveData<Resource<TeamMember>> saveTeamMember(long id, TeamMemberForm f) { return run(id == 0 ? api().addTeamMember(f) : api().updateTeamMember(id, f)); }
    public LiveData<Resource<Void>> deleteTeamMember(long id) { return run(api().deleteTeamMember(id)); }
    public LiveData<Resource<List<HireService>>> companyServices() { return run(api().companyServices()); }
    public LiveData<Resource<HireService>> saveHireService(long id, HireServiceForm f) { return run(id == 0 ? api().createHireService(f) : api().updateHireService(id, f)); }
    public LiveData<Resource<HireService>> hideHireService(long id) { return run(api().hideHireService(id)); }
    public LiveData<Resource<List<HireRequest>>> companyServiceRequests(String status) { return run(api().companyServiceRequests(status)); }
    public LiveData<Resource<HireRequest>> acceptServiceRequest(long id, HireAccept f) { return run(api().acceptServiceRequest(id, f)); }
    public LiveData<Resource<HireRequest>> declineServiceRequest(long id, String reason) { return run(api().declineServiceRequest(id, new ReasonRequest(reason))); }
    public LiveData<Resource<HireRequest>> completeServiceRequest(long id) { return run(api().completeServiceRequest(id)); }
    public LiveData<Resource<Invoice>> supplyInvoice(long id) { return run(api().supplyInvoice(id)); }
    public LiveData<Resource<BulkRequest>> updateBulkRequest(long id, BulkRequestForm f) { return run(api().updateBulkRequest(id, f)); }
    public LiveData<Resource<List<HireService>>> browseHire(String category, String q) { return run(api().browseHire(category, q)); }
    public LiveData<Resource<HireRequest>> requestHire(long id, HireRequestForm f) { return run(api().requestHire(id, f)); }
    public LiveData<Resource<List<HireRequest>>> myHireRequests() { return run(api().myHireRequests()); }
    public LiveData<Resource<HireRequest>> cancelHireRequest(long id) { return run(api().cancelHireRequest(id)); }
    public LiveData<Resource<MarketOverview>> marketOverview() { return run(api().marketOverview()); }
    public LiveData<Resource<List<SupplyOrder>>> adminSupplyOrders(String s) { return run(api().adminSupplyOrders(s)); }
    public LiveData<Resource<List<BulkRequest>>> adminProduceRequests(String s) { return run(api().adminProduceRequests(s)); }
    public LiveData<Resource<List<BulkOrder>>> adminPurchaseOrders(String s) { return run(api().adminPurchaseOrders(s)); }
    public LiveData<Resource<List<HireRequest>>> adminServiceRequests(String s) { return run(api().adminServiceRequests(s)); }

    private <T> LiveData<Resource<T>> run(Call<T> call) {
        MutableLiveData<Resource<T>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful()) live.setValue(Resource.success(r.body()));
                else live.setValue(Resource.error(errorText(r)));
            }
            @Override public void onFailure(Call<T> c, Throwable t) {
                live.setValue(Resource.error(Offline.isOnline() ? I18n.t("Network error: ") + t.getMessage() : I18n.t("No internet. Please try again when you are online.")));
            }
        });
        return live;
    }

    private static String errorText(Response<?> r) {
        try {
            String s = r.errorBody() != null ? r.errorBody().string() : "";
            JsonObject o = JsonParser.parseString(s).getAsJsonObject();
            if (o.has("message") && !o.get("message").getAsString().isEmpty()) return o.get("message").getAsString();
        } catch (Exception ignored) { }
        if (r.code() == 401) return I18n.t("Please log in again");
        if (r.code() == 403) return I18n.t("Not allowed");
        return I18n.t("Error ") + r.code();
    }
    // crop doctor
    public LiveData<Resource<CropGuide>> cropGuide() { return run(api().cropGuide()); }
    public LiveData<Resource<CropDiagnosis>> cropDiagnose(String crop, List<String> symptoms) { return run(api().cropDiagnose(new CropDiagnoseRequest(crop, symptoms))); }
    public LiveData<Resource<CropCase>> cropSend(String crop, List<String> symptoms, String description) { return run(api().cropSend(new CropCaseRequest(crop, symptoms, description))); }
    public LiveData<Resource<CropCase>> cropPhoto(long id, byte[] jpeg) {
        RequestBody body = RequestBody.create(jpeg, MediaType.parse("image/jpeg"));
        return run(api().cropPhoto(id, MultipartBody.Part.createFormData("file", "plant.jpg", body)));
    }
    public LiveData<Resource<List<CropCase>>> cropCases(String status) { return run(api().cropCases(status)); }
    public LiveData<Resource<CropCase>> cropCase(long id) { return run(api().cropCase(id)); }
    public LiveData<Resource<CropCase>> cropReview(long id, String note, String confirmed) { return run(api().cropReview(id, new CropReviewRequest(note, confirmed))); }
    public LiveData<Resource<CropCase>> cropClose(long id) { return run(api().cropClose(id)); }
    // cash payments
    public LiveData<Resource<List<CashSale>>> cashMine() { return run(api().cashMine()); }
    public LiveData<Resource<List<CashSale>>> cashForOrder(long id) { return run(api().cashForOrder(id)); }
    public LiveData<Resource<CashSale>> cashRecord(long orderId, String note) { return run(api().cashRecord(orderId, new CashNote(note))); }
    public LiveData<Resource<CashSale>> cashConfirm(long id) { return run(api().cashConfirm(id, new CashNote(null))); }
    public LiveData<Resource<CashSale>> cashDispute(long id, String note) { return run(api().cashDispute(id, new CashNote(note))); }
    public LiveData<Resource<List<CashBill>>> cashBills() { return run(api().cashBills()); }
    public LiveData<Resource<CashBill>> cashPay(long id, String phone) { return run(api().cashPay(id, new CashPayRequest(phone))); }
    // group deals
    public LiveData<Resource<List<Deal>>> deals(Long groupId, String kind) { return run(api().deals(groupId, kind)); }
    public LiveData<Resource<List<Deal>>> dealMarket() { return run(api().dealMarket()); }
    public LiveData<Resource<DealDetail>> deal(long id) { return run(api().deal(id)); }
    public LiveData<Resource<DealDetail>> dealCreate(DealRequest r) { return run(api().dealCreate(r)); }
    public LiveData<Resource<DealDetail>> dealShare(long id, double qty) { return run(api().dealShare(id, new DealShareRequest(qty))); }
    public LiveData<Resource<DealDetail>> dealClose(long id) { return run(api().dealClose(id)); }
    public LiveData<Resource<DealDetail>> dealComplete(long id, Double price, String note) { return run(api().dealComplete(id, new DealCompleteRequest(price, note))); }
    public LiveData<Resource<DealDetail>> dealCancel(long id) { return run(api().dealCancel(id)); }
    // delivery
    public LiveData<Resource<DeliveryRates>> deliveryRates() { return run(api().deliveryRates()); }
    public LiveData<Resource<DeliveryRates>> setDeliveryRates(double base, double perKm, Double factor, Double maxKm) { return run(api().setDeliveryRates(new DeliveryRatesRequest(base, perKm, factor, maxKm))); }
    public LiveData<Resource<DeliveryOptions>> deliveryOptions(long orderId, Double lat, Double lng) { return run(api().deliveryOptions(orderId, lat, lng)); }
    public LiveData<Resource<DeliveryJob>> deliveryRequest(long orderId, long transporterId, Double lat, Double lng, String note) { return run(api().deliveryRequest(new DeliveryJobRequest(orderId, transporterId, lat, lng, note))); }
    public LiveData<Resource<List<DeliveryJob>>> deliveryJobs(String status) { return run(api().deliveryJobs(status)); }
    public LiveData<Resource<DeliveryJob>> deliveryCancel(long id) { return run(api().deliveryCancel(id)); }
    public LiveData<Resource<DeliveryJob>> deliveryAccept(long id, Double km) { return run(api().deliveryAccept(id, new DeliveryAcceptRequest(km))); }
    public LiveData<Resource<DeliveryJob>> deliveryDecline(long id, String reason) { return run(api().deliveryDecline(id, new DeliveryDeclineRequest(reason))); }
    public LiveData<Resource<DeliveryJob>> deliveryPickup(long id) { return run(api().deliveryPickup(id)); }
    public LiveData<Resource<DeliveryJob>> deliveryDeliver(long id) { return run(api().deliveryDeliver(id)); }
    public LiveData<Resource<DeliveryJob>> deliveryFeeReceived(long id) { return run(api().deliveryFeeReceived(id)); }
}
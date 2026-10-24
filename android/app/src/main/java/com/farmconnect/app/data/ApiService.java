package com.farmconnect.app.data;

import com.farmconnect.app.data.Models.*;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @POST("api/auth/register") Call<AuthResponse> register(@Body RegisterRequest r);
    @POST("api/auth/login") Call<AuthResponse> login(@Body LoginRequest r);
    @POST("api/auth/google") Call<AuthResponse> google(@Body GoogleRequest r);
    @POST("api/auth/forgot-password") Call<MessageResponse> forgotPassword(@Body ForgotRequest r);
    @POST("api/auth/reset-password") Call<MessageResponse> resetPassword(@Body ResetRequest r);

    @GET("api/farms/mine") Call<List<Farm>> myFarms();
    @POST("api/farms") Call<Farm> createFarm(@Body FarmRequest r);
    @PUT("api/farms/{id}") Call<Farm> updateFarm(@Path("id") long id, @Body FarmRequest r);
    @DELETE("api/farms/{id}") Call<Void> deleteFarm(@Path("id") long id);

    @GET("api/products") Call<List<Product>> products(@Query("q") String q);
    @GET("api/products/mine") Call<List<Product>> myProducts();
    @GET("api/products/{id}") Call<Product> product(@Path("id") long id);
    @Multipart @POST("api/products/{id}/images")
    Call<Product> addProductImage(@Path("id") long id, @Part MultipartBody.Part file);
    @DELETE("api/products/{id}/images/{index}")
    Call<Product> deleteProductImage(@Path("id") long id, @Path("index") int index);
    @POST("api/products") Call<Product> createProduct(@Body ProductRequest r);
    @PUT("api/products/{id}") Call<Product> updateProduct(@Path("id") long id, @Body ProductRequest r);
    @DELETE("api/products/{id}") Call<Void> deleteProduct(@Path("id") long id);

    @GET("api/cart") Call<List<CartItem>> cart();
    @POST("api/cart") Call<CartItem> addToCart(@Body CartRequest r);
    @PUT("api/cart/{id}") Call<CartItem> setQty(@Path("id") long id, @Body QuantityRequest r);
    @DELETE("api/cart/{id}") Call<Void> removeCartItem(@Path("id") long id);

    @GET("api/wishlist") Call<List<Product>> wishlist();
    @POST("api/wishlist/{pid}") Call<Void> addWish(@Path("pid") long pid);
    @DELETE("api/wishlist/{pid}") Call<Void> removeWish(@Path("pid") long pid);

    @POST("api/orders") Call<Order> placeOrder(@Body OrderRequest r);
    // pay in the app (M-Pesa prompt) and farmer earnings
    @POST("api/payments/mpesa/{id}/stk") Call<PayStatus> startStk(@Path("id") long id, @Body PhoneRequest r);
    @GET("api/payments/{id}") Call<PayStatus> payStatus(@Path("id") long id);
    @GET("api/payments/farmer/earnings") Call<Earnings> earnings();
    @GET("api/payments/farmer/account") Call<PayoutAccount> payoutAccount();
    @PUT("api/payments/farmer/account") Call<PayoutAccount> setPayoutAccount(@Body PhoneRequest r);
    @GET("api/orders") Call<List<Order>> myOrders();
    @PATCH("api/orders/{id}/cancel") Call<Order> cancelOrder(@Path("id") long id);
    @GET("api/orders/farmer") Call<List<Order>> farmerOrders();
    @PATCH("api/orders/{id}/status") Call<Order> updateStatus(@Path("id") long id, @Body StatusRequest r);

    // farmer verification
    @GET("api/verification/me") Call<VerificationResponse> myVerification();
    @PUT("api/verification/me") Call<VerificationResponse> saveDraft(@Body DraftRequest r);
    @POST("api/verification/me/otp/send") Call<Void> sendOtp(@Body PhoneRequest r);
    @POST("api/verification/me/otp/verify") Call<VerificationResponse> verifyOtp(@Body OtpVerifyRequest r);
    @Multipart @POST("api/verification/me/documents")
    Call<VerificationResponse> uploadDoc(@Part("type") RequestBody type, @Part MultipartBody.Part file);
    @POST("api/verification/me/submit") Call<VerificationResponse> submitVerification();

    // community
    @GET("api/posts") Call<List<Post>> posts(@Query("type") String type, @Query("q") String q);
    @GET("api/posts/{id}") Call<Post> post(@Path("id") long id);
    @POST("api/posts") Call<Post> createPost(@Body PostRequest r);
    @DELETE("api/posts/{id}") Call<Void> deletePost(@Path("id") long id);
    @POST("api/posts/{id}/register") Call<Post> togglePostRegistration(@Path("id") long id);
    @GET("api/messages/conversations") Call<List<Conversation>> conversations();
    @GET("api/messages/with/{id}") Call<List<ChatMessage>> thread(@Path("id") long id);
    @POST("api/messages") Call<ChatMessage> sendMessage(@Body ChatRequest r);
    @GET("api/messages/contacts") Call<List<Contact>> contacts(@Query("q") String q);
    @GET("api/notifications") Call<List<AppNotification>> notifications();
    @POST("api/devices") Call<Void> registerDevice(@Body java.util.Map<String, String> body);
    @DELETE("api/devices") Call<Void> unregisterDevice(@Header("Authorization") String bearer, @Query("token") String token);
    @GET("api/notifications/counts") Call<Counts> counts();
    @POST("api/notifications/read-all") Call<Counts> readAllNotifications();
    @POST("api/notifications/{id}/read") Call<Counts> readNotification(@Path("id") long id);
    @GET("api/weather") Call<Weather> weather();
    @GET("api/records") Call<List<FarmRecord>> records(@Query("type") String type);
    @POST("api/records") Call<FarmRecord> createRecord(@Body RecordRequest r);
    @PUT("api/records/{id}") Call<FarmRecord> updateRecord(@Path("id") long id, @Body RecordRequest r);
    @DELETE("api/records/{id}") Call<Void> deleteRecord(@Path("id") long id);
    @GET("api/records/summary") Call<RecordSummary> recordSummary();
    @GET("api/services") Call<List<ServiceProvider>> services(@Query("category") String category, @Query("q") String q);
    @POST("api/services") Call<ServiceProvider> createService(@Body ServiceRequest r);
    @DELETE("api/services/{id}") Call<Void> deleteService(@Path("id") long id);
    @GET("api/groups") Call<List<Group>> groups(@Query("mine") Boolean mine, @Query("type") String type, @Query("q") String q);
    @GET("api/groups/{id}") Call<GroupDetail> group(@Path("id") long id);
    @POST("api/groups") Call<Group> createGroup(@Body GroupRequest r);
    @DELETE("api/groups/{id}") Call<Void> deleteGroup(@Path("id") long id);
    @POST("api/groups/{id}/join") Call<Group> joinGroup(@Path("id") long id);
    @POST("api/groups/{id}/leave") Call<Group> leaveGroup(@Path("id") long id);
    @DELETE("api/groups/{id}/members/{userId}") Call<Void> removeGroupMember(@Path("id") long id, @Path("userId") long userId);
    @POST("api/groups/{id}/announce") Call<Void> announceToGroup(@Path("id") long id, @Body AnnounceRequest r);
    @POST("api/feedback") Call<Void> sendFeedback(@Body FeedbackRequest r);
    @GET("api/analytics") Call<Analytics> analytics();
    @GET("api/analytics/report.csv") Call<okhttp3.ResponseBody> analyticsCsv();

    // prices & reviews
    @GET("api/prices") Call<List<Price>> prices(@Query("q") String q);
    @GET("api/prices/history") Call<List<PricePoint>> priceHistory(@Query("crop") String crop, @Query("market") String market);
    @POST("api/prices") Call<Price> postPrice(@Body PriceRequest r);
    @DELETE("api/prices/{id}") Call<Void> deletePrice(@Path("id") long id);
    @POST("api/prices/alerts/toggle") Call<AlertToggle> togglePriceAlert(@Query("crop") String crop);
    /** price == null clears the target (back to "every change"). direction: ABOVE or BELOW. */
    @POST("api/prices/alerts/target") Call<AlertInfo> setPriceTarget(@Query("crop") String crop, @Query("price") Double price, @Query("direction") String direction);
    @POST("api/reviews") Call<Review> createReview(@Body ReviewRequest r);
    @GET("api/reviews/seller/{id}") Call<SellerReviewSummary> sellerReviews(@Path("id") long id);
    @POST("api/reviews/{id}/reply") Call<Review> replyReview(@Path("id") long id, @Body ReplyRequest r);
    @DELETE("api/reviews/{id}") Call<Void> deleteReview(@Path("id") long id);

    // admin
    @GET("api/admin/stats") Call<AdminStats> adminStats();
    @GET("api/admin/users") Call<List<UserSummary>> adminUsers(@Query("role") String role, @Query("q") String q);
    @POST("api/admin/users/{id}/enabled") Call<UserSummary> setUserEnabled(@Path("id") long id, @Body ActiveRequest r);
    @GET("api/admin/orders") Call<List<Order>> adminOrders();
    @PATCH("api/admin/orders/{id}/status") Call<Order> adminSetOrderStatus(@Path("id") long id, @Body StatusRequest r);
    @GET("api/admin/products") Call<List<Product>> adminProducts(@Query("q") String q);
    @POST("api/admin/products/{id}/active") Call<Product> adminSetProductActive(@Path("id") long id, @Body ActiveRequest r);
    @POST("api/admin/officers") Call<AuthResponse> createOfficer(@Body OfficerRequest r);
    @POST("api/auth/change-password") Call<AuthResponse> changePassword(@Body ChangePasswordRequest r);
    @GET("api/admin/security/summary") Call<SecuritySummary> securitySummary();
    @GET("api/admin/security/audit") Call<List<AuditEntry>> securityAudit(@Query("action") String action, @Query("q") String q, @Query("limit") int limit);
    @GET("api/admin/security/locks") Call<List<LockInfo>> securityLocks();
    @POST("api/admin/security/locks/unlock") Call<Cleared> unlock(@Body UnlockRequest r);
    @POST("api/admin/security/users/{id}/force-logout") Call<MessageResponse> forceLogout(@Path("id") long id);
    @POST("api/admin/security/users/{id}/reset-password") Call<TempPassword> adminResetPassword(@Path("id") long id);
    @GET("api/admin/county-admins") Call<List<UserSummary>> countyAdmins();
    @POST("api/admin/county-admins") Call<UserSummary> createCountyAdmin(@Body CountyAdminRequest r);
    @PUT("api/admin/county-admins/{id}/county") Call<UserSummary> setCountyAdminCounty(@Path("id") long id, @Body CountyRequest r);
    @PUT("api/admin/officers/{id}/assignment") Call<UserSummary> assignOfficer(@Path("id") long id, @Body AssignmentRequest r);

    // reviewer (admin / county officer)
    @GET("api/admin/verifications") Call<List<ReviewSummary>> reviews(@Query("status") String status);
    @GET("api/admin/verifications/{id}") Call<ReviewDetail> reviewDetail(@Path("id") long id);
    @GET("api/admin/verifications/{id}/documents/{type}") Call<ResponseBody> reviewDoc(@Path("id") long id, @Path("type") String type);
    @POST("api/admin/verifications/{id}/start-review") Call<ReviewDetail> startReview(@Path("id") long id);
    @POST("api/admin/verifications/{id}/approve") Call<ReviewDetail> approve(@Path("id") long id);
    @POST("api/admin/verifications/{id}/reject") Call<ReviewDetail> reject(@Path("id") long id, @Body RejectRequest r);

    // company portal
    @GET("api/company/me") Call<CompanyView> myCompany();
    @POST("api/company/me") Call<CompanyView> registerCompany(@Body CompanyRequest r);
    @PUT("api/company/me") Call<CompanyView> updateCompany(@Body CompanyRequest r);
    @Multipart @POST("api/company/me/documents")
    Call<CompanyView> uploadCompanyDoc(@Part("type") RequestBody type, @Part MultipartBody.Part file);
    @DELETE("api/company/me/documents/{id}") Call<CompanyView> deleteCompanyDoc(@Path("id") long id);
    @GET("api/company/me/documents/{id}") Call<ResponseBody> companyDocFile(@Path("id") long id);

    // company verification (super admin only)
    @GET("api/admin/companies") Call<List<AdminCompanySummary>> adminCompanies(@Query("status") String status);
    @GET("api/admin/companies/{id}") Call<AdminCompanyDetail> adminCompany(@Path("id") long id);
    @GET("api/admin/companies/{id}/documents/{docId}") Call<ResponseBody> adminCompanyDoc(@Path("id") long id, @Path("docId") long docId);
    @POST("api/admin/companies/{id}/start-review") Call<AdminCompanyDetail> companyStartReview(@Path("id") long id);
    @POST("api/admin/companies/{id}/verify") Call<AdminCompanyDetail> companyVerify(@Path("id") long id);
    @POST("api/admin/companies/{id}/reject") Call<AdminCompanyDetail> companyReject(@Path("id") long id, @Body ReasonRequest r);
    @POST("api/admin/companies/{id}/suspend") Call<AdminCompanyDetail> companySuspend(@Path("id") long id, @Body ReasonRequest r);
    @POST("api/admin/companies/{id}/reinstate") Call<AdminCompanyDetail> companyReinstate(@Path("id") long id);

    // supplier listings and orders (company side)
    @GET("api/company/products") Call<List<SupplyProduct>> companyProducts();
    @POST("api/company/products") Call<SupplyProduct> createSupplyProduct(@Body SupplyProductRequest r);
    @PUT("api/company/products/{id}") Call<SupplyProduct> updateSupplyProduct(@Path("id") long id, @Body SupplyProductRequest r);
    @GET("api/company/supply-orders") Call<List<SupplyOrder>> companySupplyOrders(@Query("status") String status);
    @POST("api/company/supply-orders/{id}/confirm") Call<SupplyOrder> supplyConfirm(@Path("id") long id);
    @POST("api/company/supply-orders/{id}/dispatch") Call<SupplyOrder> supplyDispatch(@Path("id") long id);
    @POST("api/company/supply-orders/{id}/deliver") Call<SupplyOrder> supplyDeliver(@Path("id") long id);
    @POST("api/company/supply-orders/{id}/mark-paid") Call<SupplyOrder> supplyMarkPaid(@Path("id") long id);
    @POST("api/company/supply-orders/{id}/reject") Call<SupplyOrder> supplyReject(@Path("id") long id, @Body ReasonRequest r);
    @GET("api/company/summary") Call<SupplySummary> companySummary();

    // farmer side
    @GET("api/supplies/products") Call<List<SupplyProduct>> browseSupplies(@Query("category") String category, @Query("q") String q);
    @POST("api/supplies/orders") Call<SupplyOrder> placeSupplyOrder(@Body PlaceSupplyOrder r);
    @GET("api/supplies/orders") Call<List<SupplyOrder>> mySupplyOrders();
    @POST("api/supplies/orders/{id}/cancel") Call<SupplyOrder> cancelSupplyOrder(@Path("id") long id);

    // supply payments and payouts
    @PUT("api/company/payout-account") Call<CompanyView> setPayoutAccount(@Body PayoutAccountRequest r);
    @GET("api/company/payouts") Call<List<PayoutView>> companyPayouts();
    @POST("api/supplies/orders/{id}/pay") Call<SupplyPaymentView> paySupplyOrder(@Path("id") long id, @Body PayRequest r);
    @GET("api/supplies/payments-enabled") Call<FlagResponse> supplyPaymentsEnabled();
    @POST("api/admin/companies/{id}/payout-account/approve") Call<AdminCompanyDetail> companyApprovePayout(@Path("id") long id);
    @POST("api/admin/companies/{id}/payout-account/revoke") Call<AdminCompanyDetail> companyRevokePayout(@Path("id") long id);

    // bulk buying: company side
    @GET("api/company/requests") Call<List<BulkRequest>> companyRequests();
    @POST("api/company/requests") Call<BulkRequest> createBulkRequest(@Body BulkRequestForm f);
    @POST("api/company/requests/{id}/close") Call<BulkRequest> closeBulkRequest(@Path("id") long id);
    @GET("api/company/requests/{id}/quotations") Call<List<BulkQuote>> requestQuotations(@Path("id") long id);
    @POST("api/company/quotations/{id}/accept") Call<BulkOrder> acceptQuotation(@Path("id") long id);
    @POST("api/company/quotations/{id}/reject") Call<BulkQuote> rejectQuotation(@Path("id") long id, @Body ReasonRequest r);
    @GET("api/company/purchase-orders") Call<List<BulkOrder>> companyPurchaseOrders();
    @POST("api/company/purchase-orders/{id}/deliver") Call<BulkOrder> poDeliver(@Path("id") long id);
    @POST("api/company/purchase-orders/{id}/mark-paid") Call<BulkOrder> poMarkPaid(@Path("id") long id);
    @POST("api/company/purchase-orders/{id}/cancel") Call<BulkOrder> poCancel(@Path("id") long id, @Body ReasonRequest r);

    // bulk buying: farmer side
    @GET("api/bulk/requests") Call<List<BulkRequest>> browseBulk(@Query("produce") String produce, @Query("county") String county);
    @POST("api/bulk/requests/{id}/quotations") Call<BulkQuote> sendQuote(@Path("id") long id, @Body BulkQuoteForm f);
    @GET("api/bulk/quotations") Call<List<BulkQuote>> myQuotes();
    @POST("api/bulk/quotations/{id}/withdraw") Call<BulkQuote> withdrawQuote(@Path("id") long id);
    @GET("api/bulk/purchase-orders") Call<List<BulkOrder>> myPurchaseOrders();
    @POST("api/bulk/purchase-orders/{id}/confirm") Call<BulkOrder> poConfirm(@Path("id") long id);
    @POST("api/bulk/purchase-orders/{id}/decline") Call<BulkOrder> poDecline(@Path("id") long id, @Body ReasonRequest r);

    // team
    @GET("api/company/team") Call<List<TeamMember>> companyTeam();
    @POST("api/company/team") Call<TeamMember> addTeamMember(@Body TeamMemberForm f);
    @PUT("api/company/team/{id}") Call<TeamMember> updateTeamMember(@Path("id") long id, @Body TeamMemberForm f);
    @DELETE("api/company/team/{id}") Call<Void> deleteTeamMember(@Path("id") long id);

    // services the company offers, and requests for them
    @GET("api/company/services") Call<List<HireService>> companyServices();
    @POST("api/company/services") Call<HireService> createHireService(@Body HireServiceForm f);
    @PUT("api/company/services/{id}") Call<HireService> updateHireService(@Path("id") long id, @Body HireServiceForm f);
    @DELETE("api/company/services/{id}") Call<HireService> hideHireService(@Path("id") long id);
    @GET("api/company/service-requests") Call<List<HireRequest>> companyServiceRequests(@Query("status") String status);
    @POST("api/company/service-requests/{id}/accept") Call<HireRequest> acceptServiceRequest(@Path("id") long id, @Body HireAccept f);
    @POST("api/company/service-requests/{id}/decline") Call<HireRequest> declineServiceRequest(@Path("id") long id, @Body ReasonRequest r);
    @POST("api/company/service-requests/{id}/complete") Call<HireRequest> completeServiceRequest(@Path("id") long id);

    // invoice and request editing
    @GET("api/company/supply-orders/{id}/invoice") Call<Invoice> supplyInvoice(@Path("id") long id);
    @PUT("api/company/requests/{id}") Call<BulkRequest> updateBulkRequest(@Path("id") long id, @Body BulkRequestForm f);

    // farmers hire services
    @GET("api/marketplace/services") Call<List<HireService>> browseHire(@Query("category") String category, @Query("q") String q);
    @POST("api/marketplace/services/{id}/requests") Call<HireRequest> requestHire(@Path("id") long id, @Body HireRequestForm f);
    @GET("api/marketplace/service-requests") Call<List<HireRequest>> myHireRequests();
    @POST("api/marketplace/service-requests/{id}/cancel") Call<HireRequest> cancelHireRequest(@Path("id") long id);

    // super admin oversight
    @GET("api/admin/marketplace/overview") Call<MarketOverview> marketOverview();
    @GET("api/admin/marketplace/supply-orders") Call<List<SupplyOrder>> adminSupplyOrders(@Query("status") String status);
    @GET("api/admin/marketplace/produce-requests") Call<List<BulkRequest>> adminProduceRequests(@Query("status") String status);
    @GET("api/admin/marketplace/purchase-orders") Call<List<BulkOrder>> adminPurchaseOrders(@Query("status") String status);
    @GET("api/admin/marketplace/service-requests") Call<List<HireRequest>> adminServiceRequests(@Query("status") String status);
}

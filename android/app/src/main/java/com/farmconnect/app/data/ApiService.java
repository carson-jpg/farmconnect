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

    // admin
    @GET("api/admin/stats") Call<AdminStats> adminStats();
    @GET("api/admin/users") Call<List<UserSummary>> adminUsers(@Query("role") String role, @Query("q") String q);
    @POST("api/admin/users/{id}/enabled") Call<UserSummary> setUserEnabled(@Path("id") long id, @Body ActiveRequest r);
    @GET("api/admin/orders") Call<List<Order>> adminOrders();
    @PATCH("api/admin/orders/{id}/status") Call<Order> adminSetOrderStatus(@Path("id") long id, @Body StatusRequest r);
    @GET("api/admin/products") Call<List<Product>> adminProducts(@Query("q") String q);
    @POST("api/admin/products/{id}/active") Call<Product> adminSetProductActive(@Path("id") long id, @Body ActiveRequest r);
    @POST("api/admin/officers") Call<AuthResponse> createOfficer(@Body OfficerRequest r);

    // reviewer (admin / county officer)
    @GET("api/admin/verifications") Call<List<ReviewSummary>> reviews(@Query("status") String status);
    @GET("api/admin/verifications/{id}") Call<ReviewDetail> reviewDetail(@Path("id") long id);
    @GET("api/admin/verifications/{id}/documents/{type}") Call<ResponseBody> reviewDoc(@Path("id") long id, @Path("type") String type);
    @POST("api/admin/verifications/{id}/start-review") Call<ReviewDetail> startReview(@Path("id") long id);
    @POST("api/admin/verifications/{id}/approve") Call<ReviewDetail> approve(@Path("id") long id);
    @POST("api/admin/verifications/{id}/reject") Call<ReviewDetail> reject(@Path("id") long id, @Body RejectRequest r);
}
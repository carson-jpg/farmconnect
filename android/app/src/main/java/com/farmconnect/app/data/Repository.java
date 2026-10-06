package com.farmconnect.app.data;

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
    // farms
    public LiveData<Resource<List<Farm>>> myFarms() { return run(api().myFarms()); }
    public LiveData<Resource<Farm>> createFarm(FarmRequest r) { return run(api().createFarm(r)); }
    public LiveData<Resource<Farm>> updateFarm(long id, FarmRequest r) { return run(api().updateFarm(id, r)); }
    public LiveData<Resource<Void>> deleteFarm(long id) { return run(api().deleteFarm(id)); }
    // products
    public LiveData<Resource<List<Product>>> products(String q) { return run(api().products(q == null || q.isEmpty() ? null : q)); }
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
    public LiveData<Resource<List<Order>>> myOrders() { return run(api().myOrders()); }
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
                    else live.postValue(Resource.error("Could not read image"));
                });
            }
            @Override public void onFailure(Call<ResponseBody> c, Throwable t) {
                live.setValue(Resource.error("Network error: " + t.getMessage()));
            }
        });
        return live;
    }

    private <T> LiveData<Resource<T>> run(Call<T> call) {
        MutableLiveData<Resource<T>> live = new MutableLiveData<>();
        live.setValue(Resource.loading());
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> c, Response<T> r) {
                if (r.isSuccessful()) live.setValue(Resource.success(r.body()));
                else live.setValue(Resource.error(errorText(r)));
            }
            @Override public void onFailure(Call<T> c, Throwable t) {
                live.setValue(Resource.error("Network error: " + t.getMessage()));
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
        if (r.code() == 401) return "Please log in again";
        if (r.code() == 403) return "Not allowed";
        return "Error " + r.code();
    }
}
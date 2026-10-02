package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

public class BuyerViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<List<Product>>> products(String q) { return repo.products(q); }
    public LiveData<Resource<CartItem>> addToCart(long pid, int q) { return repo.addToCart(pid, q); }
    public LiveData<Resource<List<CartItem>>> cart() { return repo.cart(); }
    public LiveData<Resource<CartItem>> setQty(long id, int q) { return repo.setQty(id, q); }
    public LiveData<Resource<Void>> removeCartItem(long id) { return repo.removeCartItem(id); }
    public LiveData<Resource<List<Product>>> wishlist() { return repo.wishlist(); }
    public LiveData<Resource<Void>> addWish(long pid) { return repo.addWish(pid); }
    public LiveData<Resource<Void>> removeWish(long pid) { return repo.removeWish(pid); }
    public LiveData<Resource<Order>> placeOrder(String address) { return repo.placeOrder(address); }
    public LiveData<Resource<List<Order>>> orders() { return repo.myOrders(); }
    public LiveData<Resource<Order>> cancelOrder(long id) { return repo.cancelOrder(id); }
}

package com.farmconnect.app.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.farmconnect.app.data.Models.*;
import com.farmconnect.app.data.Repository;
import com.farmconnect.app.data.Resource;
import java.util.List;

public class FarmerViewModel extends ViewModel {
    private final Repository repo = Repository.get();

    public LiveData<Resource<List<Farm>>> myFarms() { return repo.myFarms(); }
    public LiveData<Resource<Farm>> createFarm(FarmRequest r) { return repo.createFarm(r); }
    public LiveData<Resource<Farm>> updateFarm(long id, FarmRequest r) { return repo.updateFarm(id, r); }
    public LiveData<Resource<Void>> deleteFarm(long id) { return repo.deleteFarm(id); }
    public LiveData<Resource<List<Product>>> products() { return repo.products(null); }
    public LiveData<Resource<List<Product>>> myProducts() { return repo.myProducts(); }
    public LiveData<Resource<Product>> product(long id) { return repo.product(id); }
    public LiveData<Resource<Product>> addProductImage(long id, byte[] jpeg) { return repo.addProductImage(id, jpeg); }
    public LiveData<Resource<Product>> deleteProductImage(long id, int index) { return repo.deleteProductImage(id, index); }
    public LiveData<Resource<Product>> createProduct(ProductRequest r) { return repo.createProduct(r); }
    public LiveData<Resource<Product>> updateProduct(long id, ProductRequest r) { return repo.updateProduct(id, r); }
    public LiveData<Resource<Void>> deleteProduct(long id) { return repo.deleteProduct(id); }
    public LiveData<Resource<List<Order>>> orders() { return repo.farmerOrders(); }
    public LiveData<Resource<Order>> updateStatus(long id, String s) { return repo.updateStatus(id, s); }
}
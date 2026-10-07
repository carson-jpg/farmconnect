package com.farmconnect.app.data;

import java.util.List;

public class Models {
    public static class AuthResponse { public String token; public long id; public String name, email, role; }
    public static class RegisterRequest {
        public String name, email, phone, password, role;
        public RegisterRequest(String name, String email, String phone, String password, String role) {
            this.name = name; this.email = email; this.phone = phone; this.password = password; this.role = role;
        }
    }
    public static class LoginRequest {
        public String email, password;
        public LoginRequest(String email, String password) { this.email = email; this.password = password; }
    }
    public static class Farm {
        public long id, ownerId; public String name, location, description; public boolean ownerVerified;
        @Override public String toString() { return name; }
    }
    public static class FarmRequest {
        public String name, location, description;
        public FarmRequest(String name, String location, String description) {
            this.name = name; this.location = location; this.description = description;
        }
    }
    public static class Product {
        public long id, farmId; public String name, summary, description, category, unit, imageUrl, farmName, farmLocation;
        public List<String> imageUrls;
        public double price; public int quantity; public boolean farmerVerified; public boolean active = true;
    }
    public static class ProductRequest {
        public String name, summary, description, category, unit, imageUrl; public double price; public int quantity; public long farmId;
        public ProductRequest(String name, String category, double price, int quantity, String unit, long farmId) {
            this.name = name; this.category = category; this.price = price; this.quantity = quantity;
            this.unit = unit; this.farmId = farmId;
        }
    }
    public static class CartRequest {
        public long productId; public int quantity;
        public CartRequest(long productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    }
    public static class QuantityRequest {
        public int quantity;
        public QuantityRequest(int quantity) { this.quantity = quantity; }
    }
    public static class CartItem { public long id; public Product product; public int quantity; }
    public static class OrderRequest {
        public String deliveryAddress, deliveryPhone, deliveryNote, paymentMethod;
        public OrderRequest(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
        public OrderRequest(String address, String phone, String note, String paymentMethod) {
            this.deliveryAddress = address; this.deliveryPhone = phone; this.deliveryNote = note; this.paymentMethod = paymentMethod;
        }
    }
    public static class StatusRequest {
        public String status;
        public StatusRequest(String status) { this.status = status; }
    }
    public static class OrderItem { public long productId; public String productName, imageUrl, farmName, unit; public int quantity; public double price; }
    public static class Order {
        public long id; public String status, deliveryAddress, createdAt, buyerName, buyerPhone, deliveryPhone, deliveryNote, paymentMethod, updatedAt;
        public double total; public List<OrderItem> items;
    }

    // ---- admin ----
    public static class ActiveRequest {
        public boolean active;
        public ActiveRequest(boolean active) { this.active = active; }
    }
    public static class OfficerRequest {
        public String name, email, phone, password;
        public OfficerRequest(String name, String email, String phone, String password) {
            this.name = name; this.email = email; this.phone = phone; this.password = password;
        }
    }
    public static class UserSummary {
        public long id; public String name, email, phone, role, createdAt; public boolean verified, enabled;
    }
    public static class AdminStats {
        public long users, farmers, verifiedFarmers, buyers, officers, pendingVerifications, underReview,
                rejectedVerifications, products, activeProducts, orders, pendingOrders, deliveredOrders, cancelledOrders;
        public double revenue;
    }

    // ---- verification ----
    public static class VerificationResponse {
        public String status, rejectionReason, fullName, maskedNationalId, phone, county, subCounty, ward, village,
                farmingType, mainProduce, submittedAt;
        public boolean phoneVerified, hasIdFront, hasIdBack, hasSelfie, hasProof, termsAccepted;
        public Double farmLat, farmLng, farmSize;
    }
    /** Null fields are not sent, so the server keeps the old value. */
    public static class DraftRequest {
        public String fullName, nationalId, subCounty, ward, village, farmingType, mainProduce;
        public Double farmLat, farmLng, farmSize;
        public Boolean termsAccepted;
    }
    public static class PhoneRequest {
        public String phone;
        public PhoneRequest(String phone) { this.phone = phone; }
    }
    public static class OtpVerifyRequest {
        public String phone, code;
        public OtpVerifyRequest(String phone, String code) { this.phone = phone; this.code = code; }
    }
    public static class RejectRequest {
        public String reason;
        public RejectRequest(String reason) { this.reason = reason; }
    }
    public static class ReviewSummary { public long id; public String fullName, subCounty, ward, status, submittedAt; }
    public static class ReviewDetail {
        public long id, farmerId;
        public String accountName, accountEmail, status, rejectionReason, fullName, nationalId, phone, county,
                subCounty, ward, village, farmingType, mainProduce, submittedAt, reviewedAt, reviewedByName;
        public boolean phoneVerified, hasIdFront, hasIdBack, hasSelfie, hasProof;
        public Double farmLat, farmLng, farmSize;
    }
}
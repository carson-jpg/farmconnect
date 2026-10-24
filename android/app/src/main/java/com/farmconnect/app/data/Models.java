package com.farmconnect.app.data;

import java.util.List;

public class Models {
    public static class AuthResponse { public String token; public long id; public String name, email, role; }
    public static class RegisterRequest {
        public String name, email, phone, password, role, confirmPassword;
        public RegisterRequest(String name, String email, String phone, String password, String role) {
            this.name = name; this.email = email; this.phone = phone; this.password = password; this.role = role;
            this.confirmPassword = password;
        }
    }
    public static class MessageResponse { public String message; }
    public static class ForgotRequest {
        public String email;
        public ForgotRequest(String email) { this.email = email; }
    }
    public static class ResetRequest {
        public String email, code, newPassword;
        public ResetRequest(String email, String code, String newPassword) {
            this.email = email; this.code = code; this.newPassword = newPassword;
        }
    }
    /** role is null on a returning user; set (FARMER/BUYER) only when creating a brand-new Google account. */
    public static class GoogleRequest {
        public String idToken, role;
        public GoogleRequest(String idToken, String role) { this.idToken = idToken; this.role = role; }
    }
    public static class LoginRequest {
        public String email, password;
        public LoginRequest(String email, String password) { this.email = email; this.password = password; }
    }
    public static class Farm {
        public long id, ownerId; public String name, location, description, subCounty; public boolean ownerVerified;
        @Override public String toString() { return name; }
    }
    public static class FarmRequest {
        public String name, location, description, subCounty;
        public FarmRequest(String name, String location, String description) {
            this.name = name; this.location = location; this.description = description;
        }
        public FarmRequest(String name, String location, String description, String subCounty) {
            this(name, location, description); this.subCounty = subCounty;
        }
    }
    public static class Product {
        public long id, farmId; public String name, summary, description, category, unit, imageUrl, farmName, farmLocation;
        public List<String> imageUrls;
        public long farmerId; public double price, sellerRating; public int quantity, sellerReviews; public boolean farmerVerified; public boolean active = true;
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
    public static class OrderItem { public long productId, farmerId; public String productName, imageUrl, farmName, unit; public int quantity; public double price; }
    public static class Order {
        public long id; public long buyerId; public String status, deliveryAddress, createdAt, buyerName, buyerPhone, deliveryPhone, deliveryNote, paymentMethod, updatedAt;
        /** UNPAID, PENDING (waiting for the buyer's PIN), PAID or FAILED. Only used for orders paid in the app. */
        public String paymentStatus, mpesaReceipt;
        public double total; public List<OrderItem> items;
    }

    // ---- payments ----
    public static class PayStatus { public long orderId; public String paymentStatus, receipt; }
    public static class Earning { public long orderId; public double amountKes, paidKes; public String status, date; }
    public static class Earnings { public double waitingKes, paidKes; public List<Earning> recent; }
    /** The farmer's payout number (masked by the server) and whether it is approved. */
    public static class PayoutAccount { public String phone, status; }

    // ---- admin ----
    public static class ActiveRequest {
        public boolean active;
        public ActiveRequest(boolean active) { this.active = active; }
    }
    public static class AssignmentRequest {
        public String county; public java.util.List<String> subCounties;
        public AssignmentRequest(String county, java.util.List<String> subCounties) { this.county = county; this.subCounties = subCounties; }
    }
    public static class OfficerRequest {
        public String name, email, phone, password;
        public OfficerRequest(String name, String email, String phone, String password) {
            this.name = name; this.email = email; this.phone = phone; this.password = password;
        }
    }
    // ---- security ----
    public static class AuditEntry { public long id; public String at, actorEmail, actorRole, action, target, detail, ip; public boolean success; }
    public static class LockInfo { public String kind, subject, until; }
    public static class SecuritySummary {
        public long failedLogins24h, blocked24h, denied24h, adminLogins24h; public int activeLocks; public boolean defaultAdminPassword;
    }
    public static class TempPassword { public String email, temporaryPassword; }
    public static class Cleared { public int cleared; }
    public static class UnlockRequest {
        public String email, ip;
        public UnlockRequest(String email, String ip) { this.email = email; this.ip = ip; }
    }
    public static class ChangePasswordRequest {
        public String currentPassword, newPassword;
        public ChangePasswordRequest(String currentPassword, String newPassword) { this.currentPassword = currentPassword; this.newPassword = newPassword; }
    }
    public static class CountyAdminRequest {
        public String name, email, phone, password, county;
        public CountyAdminRequest(String name, String email, String phone, String password, String county) {
            this.name = name; this.email = email; this.phone = phone; this.password = password; this.county = county;
        }
    }
    public static class CountyRequest {
        public String county;
        public CountyRequest(String county) { this.county = county; }
    }
    public static class UserSummary {
        public long id; public String name, email, phone, role, createdAt; public boolean verified, enabled;
        public String assignedCounty; public java.util.List<String> assignedSubCounties;
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

    // ================= community: information, messages, records, directory, groups, analytics =================
    public static class Post {
        public long id, views, registrations; public boolean registered;
        public String type, title, topic, excerpt, body, location, eventDate, contact, authorName, createdAt, audience;
    }
    public static class PostRequest {
        public String type, title, topic, body, location, eventDate, contact, audience; public Boolean notifyUsers, sendSms;
    }
    public static class ChatRequest {
        public long recipientId; public String body;
        public ChatRequest(long recipientId, String body) { this.recipientId = recipientId; this.body = body; }
    }
    public static class ChatMessage { public long id, senderId, recipientId; public String body, createdAt; public boolean mine; }
    public static class Conversation { public long userId, unread; public String name, role, lastMessage, lastAt; public boolean verified; }
    public static class Contact { public long id; public String name, role; public boolean verified; }
    public static class AppNotification { public long id, refId; public String type, title, body, createdAt; public boolean read; }
    public static class Counts { public long notifications, messages; }
    public static class WeatherDay { public String date; public double minC, maxC, rainMm; public int rainProb; }
    public static class Weather {
        public String place, updatedAt; public double tempC, windKmh, rainNowMm; public int humidity;
        public List<WeatherDay> days; public List<String> advisories;
    }
    public static class RecordRequest {
        public long farmId; public String type, title, date, unit, notes; public Double quantity, amount;
    }
    public static class FarmRecord {
        public long id, farmId; public String farmName, type, title, date, unit, notes; public Double quantity, amount;
    }
    public static class RecordSummary {
        public double income, expenses, profit, platformSales, plantedAcres; public long records;
        public java.util.Map<String, Double> harvestByCrop, livestock;
    }
    public static class ServiceRequest { public String name, category, description, services, phone, email, subCounty, location; }
    public static class ServiceProvider {
        public long id; public String name, category, description, services, phone, email, subCounty, location;
    }
    public static class GroupRequest { public String name, type, description, subCounty, location, contactPhone; }
    public static class Group {
        public long id, members, leaderId; public boolean member, leader;
        public String name, type, description, subCounty, location, contactPhone, leaderName;
    }
    public static class Member { public long userId; public String name, phone, joinedAt; public boolean leader, verified; }
    public static class GroupDetail { public Group group; public List<Member> members; }
    public static class AnnounceRequest {
        public String title, body;
        public AnnounceRequest(String title, String body) { this.title = title; this.body = body; }
    }
    public static class FeedbackRequest {
        public int rating; public String comment;
        public FeedbackRequest(int rating, String comment) { this.rating = rating; this.comment = comment; }
    }
    public static class Indicators {
        public long registeredFarmers, verifiedFarmers, registeredBuyers, activeUsers30d, groupsOnboarded, groupMembers,
                informationInteractions, marketListings, farmerBuyerConnections, opportunitiesAccessed, officersTotal,
                officersActive30d, programmesCommunicated, satisfactionResponses, farmersUsingRecords, recordsTotal, messagesSent;
        public double satisfactionAverage;
    }
    public static class Analytics {
        public Indicators indicators;
        public java.util.Map<String, Long> farmersBySubCounty, verifiedBySubCounty, productsByCategory, groupsByType,
                membersByGroup, ordersByMonth, postsByType, harvestKgByCrop;
        public java.util.Map<String, Double> salesByMonth;
        public java.util.Map<String, Long> farmersByCounty, verifiedByCounty;
        public boolean scoped; public String scopeLabel;
    }

    // ================= market prices & seller reviews =================
    public static class PriceRequest { public String crop, market, unit, date; public Double price; }
    public static class Price {
        public long id; public String crop, market, unit, date, postedBy; public double price;
        public Double previousPrice, changePct; public boolean subscribed;
        /** My target for this crop (KES) and "ABOVE"/"BELOW"; null when I only get plain "every change" alerts. */
        public Double alertTarget; public String alertDirection;
    }
    public static class PricePoint { public long id; public String date, unit, market; public double price; }
    public static class AlertToggle { public String crop; public boolean subscribed; }
    public static class AlertInfo { public String crop, direction; public Double targetPrice; }
    public static class ReviewRequest {
        public long orderId, sellerId; public int rating; public String comment;
        public ReviewRequest(long orderId, long sellerId, int rating, String comment) {
            this.orderId = orderId; this.sellerId = sellerId; this.rating = rating; this.comment = comment;
        }
    }
    public static class ReplyRequest { public String reply; public ReplyRequest(String r) { reply = r; } }
    public static class Review { public long id; public int rating; public String buyerName, comment, reply, createdAt, repliedAt; }
    public static class SellerReviewSummary {
        public long sellerId; public String sellerName; public boolean verified; public double average; public int count;
        public int[] distribution; public List<Review> reviews;
    }

    // ---- companies ----
    public static class CompanyRequest {
        public String name, businessRegNumber, kraPin, category, repName, repPhone, repEmail, description, address, county;
        public boolean canBuy, canSell;
        public List<String> operatingCounties;
    }
    public static class CompanyDocument { public long id; public String type, fileName, uploadedAt; }
    public static class CompanyView {
        public long id; public String name, businessRegNumber, kraPin, category, repName, repPhone, repEmail, description, address, county;
        public String status, rejectionReason, suspensionReason, submittedAt, reviewedAt;
        public boolean canBuy, canSell, verified;
        public String payoutType, payoutAccount, payoutReference; public boolean payoutApproved;
        public List<String> operatingCounties; public List<CompanyDocument> documents;
    }
    public static class AdminCompanySummary { public long id; public String name, category, county, status, submittedAt; public int documents; }
    public static class AdminCompanyDetail { public CompanyView company; public String ownerName, ownerEmail, ownerPhone, reviewedByName; }
    public static class ReasonRequest { public String reason; public ReasonRequest(String r) { reason = r; } }

    // ---- supplier listings and input orders ----
    public static class SupplyProductRequest {
        public String name, category, description, unit, licenceRef;
        public double price; public int stockQty, minOrderQty; public boolean active;
    }
    public static class SupplyProduct {
        public long id, companyId; public String companyName, county, name, category, description, unit, licenceRef;
        public double price; public int stockQty, minOrderQty; public boolean active;
    }
    public static class SupplyLine { public long productId; public int quantity; public SupplyLine(long p, int q) { productId = p; quantity = q; } }
    public static class PlaceSupplyOrder { public List<SupplyLine> items; public String deliveryAddress, deliveryPhone, deliveryNote; }
    public static class SupplyItem { public long productId; public String name, unit; public int quantity; public double unitPrice, lineTotal; }
    public static class SupplyOrder {
        public long id, companyId; public String status, paymentStatus, companyName, buyerName, deliveryPhone, deliveryAddress, deliveryNote;
        public String rejectionReason, createdAt, deliveredAt, paidAt, paymentMethod, mpesaReceipt;
        public List<SupplyItem> items; public double total; public Double platformFee, countyFee, companyNet;
    }
    public static class SupplySummary {
        public int newOrders, inProgress, delivered, activeProducts, lowStock;
        public double grossDelivered, feesDelivered, netDelivered, netUnpaid;
    }

    // ---- supply payments and payouts ----
    public static class PayoutAccountRequest { public String type, account, reference; }
    public static class PayoutView { public long id, orderId, amountKes; public String companyName, status, failureReason, providerTransactionId, createdAt, completedAt; }
    public static class PayRequest { public String phone; public PayRequest(String p) { phone = p; } }
    public static class SupplyPaymentView { public long id, orderId, expectedKes, paidKes, excessKes; public String buyerName, status, receipt, phone, note, createdAt; }
    public static class FlagResponse { public boolean enabled; }

    // ---- bulk buying ----
    public static class BulkRequestForm {
        public String produce, description, unit, deliveryCounty, deliveryLocation, neededBy, closesAt;
        public double quantity; public Double maxPrice;
    }
    public static class BulkRequest {
        public long id, companyId, openQuotations;
        public String companyName, companyCounty, produce, description, unit, deliveryCounty, deliveryLocation, neededBy, closesAt, status, createdAt;
        public double quantity, remaining; public Double maxPrice;
    }
    public static class BulkQuoteForm { public double quantity, pricePerUnit; public String availableFrom, message; }
    public static class BulkQuote {
        public long id, requestId;
        public String produce, unit, companyName, farmerName, farmerPhone, availableFrom, message, status, rejectionReason, createdAt;
        public double quantity, pricePerUnit, total;
    }
    public static class BulkOrder {
        public long id, requestId;
        public String produce, unit, companyName, farmerName, contactName, contactPhone, deliveryCounty, deliveryLocation, neededBy;
        public String status, paymentStatus, cancelReason, createdAt, deliveredAt, paidAt;
        public double quantity, pricePerUnit, total; public Double platformFee, countyFee, farmerNet;
    }

    // ---- team, services, invoices, admin overview ----
    public static class TeamMemberForm { public String name, phone, email, role; public boolean active; }
    public static class TeamMember { public long id; public String name, phone, email, role, createdAt; public boolean active; }
    public static class HireServiceForm {
        public String name, category, description, priceBasis; public Double price; public List<String> counties; public boolean active;
    }
    public static class HireService {
        public long id, companyId; public String companyName, companyCounty, name, category, description, priceBasis;
        public Double price; public List<String> counties; public boolean active;
    }
    public static class HireRequestForm { public String details, preferredDate, location, phone; }
    public static class HireAccept { public Double quotedPrice; public String note; }
    public static class HireRequest {
        public long id, serviceId;
        public String serviceName, category, companyName, companyPhone, farmerName, phone, details, preferredDate, location, status, companyNote, createdAt, updatedAt;
        public Double quotedPrice;
    }
    public static class InvoiceLine { public String description, unit; public int quantity; public double unitPrice, lineTotal; }
    public static class Invoice {
        public String number, issuedAt, sellerName, sellerRegNumber, sellerKraPin, sellerAddress, sellerPhone, sellerEmail;
        public String buyerName, buyerPhone, deliveryAddress, paymentStatus, paymentMethod, receipt, paidAt, orderStatus;
        public List<InvoiceLine> lines; public double total;
    }
    public static class MarketOverview {
        public java.util.Map<String, Long> companies, supplyOrders, produceRequests, quotations, purchaseOrders, serviceRequests;
        public double supplyDeliveredGross;
    }
}

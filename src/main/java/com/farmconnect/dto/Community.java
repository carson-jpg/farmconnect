package com.farmconnect.dto;

import com.farmconnect.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request / response records for the community features: information, messages, records, directory, groups, analytics. */
public final class Community {
    private Community() {}

    // ---- information & programmes
    public record PostRequest(@NotNull PostType type, @NotBlank @Size(max = 200) String title, String topic,
                              @NotBlank @Size(max = 6000) String body, String location, Instant eventDate,
                              String contact, String audience, Boolean notifyUsers, Boolean sendSms) {}
    public record PostResponse(Long id, PostType type, String title, String topic, String excerpt, String body,
                               String location, Instant eventDate, String contact, String authorName,
                               Instant createdAt, long views, long registrations, boolean registered, String audience) {}

    // ---- messages & notifications
    public record ChatRequest(@NotNull Long recipientId, @NotBlank @Size(max = 1000) String body) {}
    public record ChatMessageResponse(Long id, Long senderId, Long recipientId, String body, Instant createdAt, boolean mine) {}
    public record ConversationResponse(Long userId, String name, Role role, boolean verified, String lastMessage,
                                       Instant lastAt, long unread) {}
    public record ContactResponse(Long id, String name, Role role, boolean verified) {}
    public record NotificationResponse(Long id, String type, String title, String body, Long refId,
                                       Instant createdAt, boolean read) {}
    public record Counts(long notifications, long messages) {}

    // ---- weather
    public record WeatherDay(String date, double minC, double maxC, double rainMm, int rainProb) {}
    public record WeatherResponse(String place, double tempC, int humidity, double windKmh, double rainNowMm,
                                  List<WeatherDay> days, List<String> advisories, Instant updatedAt) {}

    // ---- farm records
    public record RecordRequest(@NotNull Long farmId, @NotNull RecordType type, @NotBlank String title, LocalDate date,
                                BigDecimal quantity, String unit, BigDecimal amount, @Size(max = 500) String notes) {}
    public record RecordResponse(Long id, Long farmId, String farmName, RecordType type, String title, LocalDate date,
                                 BigDecimal quantity, String unit, BigDecimal amount, String notes) {}
    public record RecordSummary(BigDecimal income, BigDecimal expenses, BigDecimal profit, BigDecimal platformSales,
                                BigDecimal plantedAcres, Map<String, BigDecimal> harvestByCrop,
                                Map<String, BigDecimal> livestock, long records) {}

    // ---- directory & groups
    public record ServiceRequest(@NotBlank String name, @NotNull ServiceCategory category, @Size(max = 1000) String description,
                                 @Size(max = 500) String services, String phone, String email, String subCounty, String location) {}
    public record ServiceResponse(Long id, String name, ServiceCategory category, String description, String services,
                                  String phone, String email, String subCounty, String location) {}
    public record GroupRequest(@NotBlank String name, @NotNull GroupType type, @Size(max = 1000) String description,
                               String subCounty, String location, String contactPhone) {}
    public record GroupResponse(Long id, String name, GroupType type, String description, String subCounty, String location,
                                String contactPhone, long members, String leaderName, Long leaderId, boolean member, boolean leader) {}
    public record MemberResponse(Long userId, String name, boolean leader, boolean verified, String phone, Instant joinedAt) {}
    public record GroupDetail(GroupResponse group, List<MemberResponse> members) {}
    public record AnnounceRequest(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 500) String body) {}

    // ---- feedback & analytics
    public record FeedbackRequest(@Min(1) @Max(5) int rating, @Size(max = 500) String comment) {}
    public record Indicators(long registeredFarmers, long verifiedFarmers, long registeredBuyers, long activeUsers30d,
                             long groupsOnboarded, long groupMembers, long informationInteractions, long marketListings,
                             long farmerBuyerConnections, long opportunitiesAccessed, long officersTotal,
                             long officersActive30d, long programmesCommunicated, double satisfactionAverage,
                             long satisfactionResponses, long farmersUsingRecords, long recordsTotal, long messagesSent) {}
    public record Analytics(Indicators indicators, Map<String, Long> farmersBySubCounty, Map<String, Long> verifiedBySubCounty,
                            Map<String, Long> productsByCategory, Map<String, Long> groupsByType,
                            Map<String, Long> membersByGroup, Map<String, Long> ordersByMonth,
                            Map<String, BigDecimal> salesByMonth, Map<String, Long> postsByType,
                            Map<String, Long> harvestKgByCrop) {}
}

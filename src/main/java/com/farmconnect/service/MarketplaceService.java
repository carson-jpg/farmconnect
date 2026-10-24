package com.farmconnect.service;

import com.farmconnect.dto.MarketDtos.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.CompanyServiceRepository;
import com.farmconnect.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Services companies offer farmers (transport, machinery hire, soil testing...) and the requests farmers send for them.
 * Only VERIFIED companies that sell can list; only their active listings are visible to farmers.
 */
@Service
@RequiredArgsConstructor
public class MarketplaceService {
    private final CompanyServiceRepository services;
    private final ServiceRequestRepository requests;
    private final CompanyService companies;
    private final NotificationService notifications;

    // ------------------------------------------------------------------ company: listings

    @Transactional(readOnly = true)
    public List<ServiceView> myServices(User owner) {
        return services.findByCompanyIdOrderByNameAsc(companies.entityOf(owner).getId()).stream().map(MarketplaceService::view).toList();
    }

    @Transactional
    public ServiceView create(User owner, ServiceForm f) {
        CompanyServiceListing s = new CompanyServiceListing();
        s.setCompany(seller(owner));
        apply(s, f);
        return view(services.save(s));
    }

    @Transactional
    public ServiceView update(User owner, Long id, ServiceForm f) {
        CompanyServiceListing s = ownService(seller(owner), id);
        apply(s, f);
        s.setUpdatedAt(Instant.now());
        return view(services.save(s));
    }

    @Transactional
    public ServiceView deactivate(User owner, Long id) {
        CompanyServiceListing s = ownService(seller(owner), id);
        s.setActive(false);
        s.setUpdatedAt(Instant.now());
        return view(services.save(s));
    }

    // ------------------------------------------------------------------ farmer

    @Transactional(readOnly = true)
    public List<ServiceView> browse(ServiceCategory category, String q, String county) {
        String like = "%" + (q == null ? "" : q.trim().replace("%", "").replace("_", "")) + "%";
        String k = county == null ? "" : county.trim().toLowerCase(Locale.ROOT);
        return services.browse(like).stream().filter(s -> category == null || s.getCategory() == category)
                .filter(s -> k.isEmpty() || csv(s.getCounties()).isEmpty()
                        || csv(s.getCounties()).stream().anyMatch(c -> c.toLowerCase(Locale.ROOT).equals(k)))
                .map(MarketplaceService::view).toList();
    }

    @Transactional
    public ServiceRequestView request(User farmer, Long serviceId, ServiceRequestForm f) {
        CompanyServiceListing s = services.findById(serviceId).filter(x -> x.isActive() && x.getCompany().getStatus() == CompanyStatus.VERIFIED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This service is not available"));
        if (requests.existsByServiceIdAndFarmerIdAndStatusIn(s.getId(), farmer.getId(), EnumSet.of(ServiceRequestStatus.REQUESTED, ServiceRequestStatus.ACCEPTED)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an open request for this service");
        CompanyServiceRequest r = new CompanyServiceRequest();
        r.setService(s);
        r.setCompany(s.getCompany());
        r.setFarmer(farmer);
        r.setDetails(f.details().trim());
        r.setPreferredDate(f.preferredDate());
        r.setLocation(f.location().trim());
        r.setPhone(f.phone().trim());
        r = requests.save(r);
        notifications.notify(s.getCompany().getOwner(), "SERVICE", "New service request",
                farmer.getName() + " asked for " + s.getName() + ".", r.getId());
        return view(r);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> myRequests(User farmer) {
        return requests.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream().map(MarketplaceService::view).toList();
    }

    @Transactional
    public ServiceRequestView cancel(User farmer, Long id) {
        CompanyServiceRequest r = requests.lockById(id).filter(x -> x.getFarmer().getId().equals(farmer.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (r.getStatus() != ServiceRequestStatus.REQUESTED && r.getStatus() != ServiceRequestStatus.ACCEPTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is already " + r.getStatus());
        r.setStatus(ServiceRequestStatus.CANCELLED);
        r.setUpdatedAt(Instant.now());
        notifications.notify(r.getCompany().getOwner(), "SERVICE", "Request cancelled", r.getFarmer().getName() + " cancelled the request for " + r.getService().getName() + ".", r.getId());
        return view(requests.save(r));
    }

    // ------------------------------------------------------------------ company: requests

    @Transactional(readOnly = true)
    public List<ServiceRequestView> companyRequests(User owner, ServiceRequestStatus status) {
        Company c = companies.entityOf(owner);
        return requests.findByCompanyIdOrderByCreatedAtDesc(c.getId()).stream()
                .filter(r -> status == null || r.getStatus() == status).map(MarketplaceService::view).toList();
    }

    @Transactional
    public ServiceRequestView accept(User owner, Long id, AcceptForm f) {
        CompanyServiceRequest r = ownRequest(owner, id);
        step(r, ServiceRequestStatus.REQUESTED, ServiceRequestStatus.ACCEPTED);
        r.setQuotedPrice(f != null && f.quotedPrice() != null ? f.quotedPrice().setScale(2, RoundingMode.HALF_UP) : r.getService().getPrice());
        r.setCompanyNote(f == null || f.note() == null || f.note().isBlank() ? null : f.note().trim());
        notifications.notify(r.getFarmer(), "SERVICE", "Service request accepted",
                r.getCompany().getName() + " accepted your request for " + r.getService().getName() + ".", r.getId());
        return view(requests.save(r));
    }

    @Transactional
    public ServiceRequestView decline(User owner, Long id, String reason) {
        CompanyServiceRequest r = ownRequest(owner, id);
        step(r, ServiceRequestStatus.REQUESTED, ServiceRequestStatus.DECLINED);
        r.setCompanyNote(reason.trim());
        notifications.notify(r.getFarmer(), "SERVICE", "Service request declined", "Reason: " + reason.trim(), r.getId());
        return view(requests.save(r));
    }

    @Transactional
    public ServiceRequestView complete(User owner, Long id) {
        CompanyServiceRequest r = ownRequest(owner, id);
        step(r, ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.COMPLETED);
        notifications.notify(r.getFarmer(), "SERVICE", "Service completed", r.getCompany().getName() + " marked " + r.getService().getName() + " as completed.", r.getId());
        return view(requests.save(r));
    }

    // ------------------------------------------------------------------ super admin

    @Transactional(readOnly = true)
    public List<ServiceRequestView> adminRequests(ServiceRequestStatus status) {
        return (status == null ? requests.findTop200ByOrderByCreatedAtDesc() : requests.findTop200ByStatusOrderByCreatedAtDesc(status))
                .stream().map(MarketplaceService::view).toList();
    }

    // ------------------------------------------------------------------ helpers

    private Company seller(User owner) {
        Company c = companies.requireVerified(owner);
        if (!c.isCanSell()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is not set up to sell products or services");
        return c;
    }

    private CompanyServiceListing ownService(Company c, Long id) {
        return services.findById(id).filter(s -> s.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    private CompanyServiceRequest ownRequest(User owner, Long id) {
        Company c = companies.entityOf(owner);
        return requests.lockById(id).filter(r -> r.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    private static void step(CompanyServiceRequest r, ServiceRequestStatus from, ServiceRequestStatus to) {
        if (r.getStatus() != from) throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is " + r.getStatus() + ", not " + from);
        r.setStatus(to);
        r.setUpdatedAt(Instant.now());
    }

    private static List<String> csv(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    private static void apply(CompanyServiceListing s, ServiceForm f) {
        s.setName(f.name().trim());
        s.setCategory(f.category());
        s.setDescription(f.description() == null || f.description().isBlank() ? null : f.description().trim());
        s.setPriceBasis(f.priceBasis() == null || f.priceBasis().isBlank() ? null : f.priceBasis().trim());
        s.setPrice(f.price() == null ? null : f.price().setScale(2, RoundingMode.HALF_UP));
        s.setCounties(f.counties() == null ? "" : String.join(",", f.counties().stream().map(String::trim).filter(x -> !x.isEmpty()).distinct().toList()));
        s.setActive(f.active());
    }

    static ServiceView view(CompanyServiceListing s) {
        return new ServiceView(s.getId(), s.getCompany().getId(), s.getCompany().getName(), s.getCompany().getCounty(), s.getName(),
                s.getCategory(), s.getDescription(), s.getPriceBasis(), s.getPrice(), csv(s.getCounties()), s.isActive());
    }

    static ServiceRequestView view(CompanyServiceRequest r) {
        return new ServiceRequestView(r.getId(), r.getService().getId(), r.getService().getName(), r.getService().getCategory(),
                r.getCompany().getName(), r.getCompany().getRepPhone(), r.getFarmer().getName(), r.getPhone(), r.getDetails(),
                r.getPreferredDate(), r.getLocation(), r.getStatus(), r.getQuotedPrice(), r.getCompanyNote(), r.getCreatedAt(), r.getUpdatedAt());
    }
}

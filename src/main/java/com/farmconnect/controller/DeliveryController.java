package com.farmconnect.controller;

import com.farmconnect.dto.DeliveryDtos.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.DeliveryFees;
import com.farmconnect.service.Locations;
import com.farmconnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Delivery. A buyer picks a verified transporter for an order; the fee is a base fee plus a rate per km (both set by the
 * super admin) for the distance from the farm to the buyer's delivery point. The job is tracked from request to delivery.
 * The fee is paid between buyer and transporter; FarmConnect only records it.
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {
    private static final List<String> ACTIVE = List.of(DeliveryJob.REQUESTED, DeliveryJob.ACCEPTED, DeliveryJob.PICKED_UP);

    private final DeliverySettingsRepository settingsRepo;
    private final DeliveryJobRepository jobs;
    private final OrderRepository orders;
    private final CompanyRepository companies;
    private final FarmerVerificationRepository verifications;
    private final NotificationService notifier;

    // ------------------------------------------------------------------ price list

    @GetMapping("/settings")
    public Rates rates() { return toRates(settings()); }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Rates setRates(@Valid @RequestBody RatesRequest r, @AuthenticationPrincipal User me) {
        DeliverySettings s = settings();
        s.setBaseFee(r.baseFee());
        s.setPerKmRate(r.perKmRate());
        if (r.roadFactor() != null) s.setRoadFactor(r.roadFactor());
        if (r.maxKm() != null) s.setMaxKm(r.maxKm());
        s.setUpdatedAt(Instant.now());
        s.setUpdatedBy(me.getEmail());
        return toRates(settingsRepo.save(s));
    }

    // ------------------------------------------------------------------ buyer: options and request

    /** The quote for an order, and the verified transporters that serve the farm's county. */
    @GetMapping("/options")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional(readOnly = true)
    public Options options(@RequestParam Long orderId, @RequestParam(required = false) Double dropLat,
                           @RequestParam(required = false) Double dropLng, @AuthenticationPrincipal User me) {
        CustomerOrder o = myOrder(orderId, me);
        DeliverySettings s = settings();
        Pickup p = pickupOf(o);
        Quote q = quote(p, dropLat, dropLng, s);
        String county = p.subCounty == null ? null : Locations.countyOf(p.subCounty);
        List<TransporterInfo> list = new ArrayList<>();
        for (Company c : companies.findAll()) {
            if (c.getCategory() != CompanyCategory.TRANSPORT || c.getStatus() != CompanyStatus.VERIFIED) continue;
            if (!serves(c, county)) continue;
            list.add(new TransporterInfo(c.getId(), c.getName(), c.getRepPhone(), c.getCounty(), c.getOperatingCounties(), c.getDescription(),
                    jobs.countByTransporterIdAndStatus(c.getId(), DeliveryJob.DELIVERED)));
            if (list.size() >= 100) break;
        }
        list.sort(Comparator.comparingLong(TransporterInfo::delivered).reversed().thenComparing(TransporterInfo::name));
        Long active = jobs.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(j -> ACTIVE.contains(j.getStatus())).map(DeliveryJob::getId).findFirst().orElse(null);
        return new Options(q, list, active);
    }

    @PostMapping("/jobs")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public JobView request(@Valid @RequestBody JobRequest r, @AuthenticationPrincipal User me) {
        CustomerOrder o = myOrder(r.orderId(), me);
        if (jobs.existsByOrderIdAndStatusIn(o.getId(), ACTIVE))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order already has a delivery in progress");
        Company c = companies.findById(r.transporterId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transporter not found"));
        if (c.getCategory() != CompanyCategory.TRANSPORT || c.getStatus() != CompanyStatus.VERIFIED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a verified transporter");
        DeliverySettings s = settings();
        Pickup p = pickupOf(o);
        boolean drop = DeliveryFees.validKenyaPoint(r.dropLat(), r.dropLng());
        DeliveryJob j = new DeliveryJob();
        j.setOrder(o);
        j.setBuyer(me);
        j.setFarmer(p.farmer);
        j.setTransporter(c);
        j.setPickupPlace(p.place);
        j.setPickupLat(p.lat);
        j.setPickupLng(p.lng);
        j.setDropAddress(o.getDeliveryAddress());
        if (drop) { j.setDropLat(r.dropLat()); j.setDropLng(r.dropLng()); }
        j.setBaseFee(s.getBaseFee());
        j.setPerKmRate(s.getPerKmRate());
        if (drop && DeliveryFees.validKenyaPoint(p.lat, p.lng)) {
            double km = DeliveryFees.roadKm(p.lat, p.lng, r.dropLat(), r.dropLng(), s.getRoadFactor());
            if (km > s.getMaxKm()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too far for delivery (limit " + (long) s.getMaxKm() + " km)");
            j.setDistanceKm(km);
            j.setFee(DeliveryFees.fee(s.getBaseFee(), s.getPerKmRate(), km));
        }
        j.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        j = jobs.save(j);
        notifier.notify(c.getOwner(), "DELIVERY", "New delivery request",
                "Order #" + o.getId() + (p.place == null ? "" : " from " + p.place)
                        + (j.getFee() == null ? "" : ". Fee KES " + j.getFee().toPlainString()), j.getId());
        return view(j, me);
    }

    @PostMapping("/jobs/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
    public JobView cancel(@PathVariable Long id, @AuthenticationPrincipal User me) {
        DeliveryJob j = find(id);
        if (!j.getBuyer().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your delivery");
        if (!DeliveryJob.REQUESTED.equals(j.getStatus()) && !DeliveryJob.ACCEPTED.equals(j.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This delivery can no longer be cancelled");
        j.setStatus(DeliveryJob.CANCELLED);
        jobs.save(j);
        notifier.notify(j.getTransporter().getOwner(), "DELIVERY", "Delivery cancelled", "Order #" + j.getOrder().getId() + " was cancelled by the buyer.", j.getId());
        return view(j, me);
    }

    // ------------------------------------------------------------------ everyone: list and open

    @GetMapping("/jobs")
    @Transactional(readOnly = true)
    public List<JobView> list(@RequestParam(required = false) String status, @AuthenticationPrincipal User me) {
        List<DeliveryJob> found;
        switch (me.getRole()) {
            case BUYER -> found = jobs.findByBuyerIdOrderByCreatedAtDesc(me.getId());
            case FARMER -> found = jobs.findByFarmerIdOrderByCreatedAtDesc(me.getId());
            case COMPANY -> found = companies.findByOwnerId(me.getId()).map(c -> jobs.findByTransporterIdOrderByCreatedAtDesc(c.getId())).orElse(List.of());
            case ADMIN -> found = jobs.findTop300ByOrderByCreatedAtDesc();
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Deliveries are for buyers, farmers, transporters and the super admin");
        }
        return found.stream()
                .filter(j -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || j.getStatus().equalsIgnoreCase(status))
                .map(j -> view(j, me)).toList();
    }

    @GetMapping("/jobs/{id}")
    @Transactional(readOnly = true)
    public JobView one(@PathVariable Long id, @AuthenticationPrincipal User me) {
        DeliveryJob j = find(id);
        requireParty(j, me);
        return view(j, me);
    }

    // ------------------------------------------------------------------ transporter

    @PostMapping("/jobs/{id}/accept")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional
    public JobView accept(@PathVariable Long id, @RequestBody(required = false) AcceptRequest r, @AuthenticationPrincipal User me) {
        DeliveryJob j = mine(id, me);
        if (!DeliveryJob.REQUESTED.equals(j.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This request was already answered");
        if (j.getDistanceKm() == null) {
            DeliverySettings s = settings();
            if (r == null || r.distanceKm() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the distance in km for this delivery");
            if (r.distanceKm() > s.getMaxKm()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too far for delivery (limit " + (long) s.getMaxKm() + " km)");
            j.setDistanceKm(Math.round(r.distanceKm() * 10.0) / 10.0);
            j.setFee(DeliveryFees.fee(j.getBaseFee(), j.getPerKmRate(), j.getDistanceKm()));
        }
        j.setStatus(DeliveryJob.ACCEPTED);
        j.setAcceptedAt(Instant.now());
        jobs.save(j);
        String msg = j.getTransporter().getName() + " will deliver order #" + j.getOrder().getId() + ". " + km(j) + ", fee KES " + j.getFee().toPlainString();
        notifier.notify(j.getBuyer(), "DELIVERY", "Delivery accepted", msg, j.getId());
        notifier.notify(j.getFarmer(), "DELIVERY", "A transporter will collect an order", msg, j.getId());
        return view(j, me);
    }

    @PostMapping("/jobs/{id}/decline")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional
    public JobView decline(@PathVariable Long id, @Valid @RequestBody DeclineRequest r, @AuthenticationPrincipal User me) {
        DeliveryJob j = mine(id, me);
        if (!DeliveryJob.REQUESTED.equals(j.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This request was already answered");
        j.setStatus(DeliveryJob.DECLINED);
        j.setDeclineReason(r.reason() == null || r.reason().isBlank() ? null : r.reason().trim());
        jobs.save(j);
        notifier.notify(j.getBuyer(), "DELIVERY", "Delivery declined", j.getTransporter().getName() + " cannot deliver order #" + j.getOrder().getId()
                + (j.getDeclineReason() == null ? "" : ": " + j.getDeclineReason()) + ". Please choose another transporter.", j.getId());
        return view(j, me);
    }

    @PostMapping("/jobs/{id}/pickup")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional
    public JobView pickedUp(@PathVariable Long id, @AuthenticationPrincipal User me) {
        DeliveryJob j = mine(id, me);
        if (!DeliveryJob.ACCEPTED.equals(j.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Accept the job before collecting");
        j.setStatus(DeliveryJob.PICKED_UP);
        j.setPickedUpAt(Instant.now());
        jobs.save(j);
        notifier.notify(j.getBuyer(), "DELIVERY", "Your order is on the way", "Order #" + j.getOrder().getId() + " was collected by " + j.getTransporter().getName() + ".", j.getId());
        notifier.notify(j.getFarmer(), "DELIVERY", "Order collected", "Order #" + j.getOrder().getId() + " was collected by " + j.getTransporter().getName() + ".", j.getId());
        return view(j, me);
    }

    @PostMapping("/jobs/{id}/deliver")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional
    public JobView delivered(@PathVariable Long id, @AuthenticationPrincipal User me) {
        DeliveryJob j = mine(id, me);
        if (!DeliveryJob.PICKED_UP.equals(j.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Collect the order before marking it delivered");
        j.setStatus(DeliveryJob.DELIVERED);
        j.setDeliveredAt(Instant.now());
        jobs.save(j);
        notifier.notify(j.getBuyer(), "DELIVERY", "Delivered", "Order #" + j.getOrder().getId() + " was delivered. Please confirm it in your orders. The delivery fee is KES "
                + j.getFee().toPlainString() + " (pay " + j.getTransporter().getName() + " directly).", j.getId());
        notifier.notify(j.getFarmer(), "DELIVERY", "Delivered", "Order #" + j.getOrder().getId() + " reached the buyer.", j.getId());
        return view(j, me);
    }

    @PostMapping("/jobs/{id}/fee-received")
    @PreAuthorize("hasRole('COMPANY')")
    @Transactional
    public JobView feeReceived(@PathVariable Long id, @AuthenticationPrincipal User me) {
        DeliveryJob j = mine(id, me);
        if (!DeliveryJob.DELIVERED.equals(j.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a delivered job can be marked paid");
        if (!j.isFeePaid()) { j.setFeePaid(true); j.setFeePaidAt(Instant.now()); jobs.save(j); }
        return view(j, me);
    }

    // ------------------------------------------------------------------ helpers

    private DeliverySettings settings() { return settingsRepo.findById(1L).orElseGet(DeliverySettings::new); }

    private static Rates toRates(DeliverySettings s) { return new Rates(s.getBaseFee(), s.getPerKmRate(), s.getRoadFactor(), s.getMaxKm()); }

    private DeliveryJob find(Long id) { return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found")); }

    /** A job addressed to the caller's own transport company. */
    private DeliveryJob mine(Long id, User me) {
        DeliveryJob j = find(id);
        Company c = companies.findByOwnerId(me.getId()).orElse(null);
        if (c == null || !c.getId().equals(j.getTransporter().getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This job is not yours");
        if (c.getStatus() != CompanyStatus.VERIFIED) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your company is not verified");
        return j;
    }

    private void requireParty(DeliveryJob j, User me) {
        boolean ok = me.getRole() == Role.ADMIN || j.getBuyer().getId().equals(me.getId()) || j.getFarmer().getId().equals(me.getId())
                || j.getTransporter().getOwner().getId().equals(me.getId());
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot open this delivery");
    }

    private CustomerOrder myOrder(Long orderId, User me) {
        CustomerOrder o = orders.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!o.getBuyer().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your order");
        if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.DELIVERED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order is already " + o.getStatus().name().toLowerCase());
        if (o.getItems().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "This order has no items");
        return o;
    }

    private static boolean serves(Company c, String county) {
        if (county == null) return true;
        String ops = c.getOperatingCounties();
        if ((ops == null || ops.isBlank()) && (c.getCounty() == null || c.getCounty().isBlank())) return true;
        if (county.equalsIgnoreCase(c.getCounty())) return true;
        if (ops != null) for (String x : ops.split(",")) if (county.equalsIgnoreCase(x.trim())) return true;
        return false;
    }

    private static String km(DeliveryJob j) { return j.getDistanceKm() == null ? "distance not set" : j.getDistanceKm() + " km"; }

    /** Where the produce is collected: the first item's farm. */
    private record Pickup(User farmer, String place, String subCounty, Double lat, Double lng) {}

    private Pickup pickupOf(CustomerOrder o) {
        Farm f = o.getItems().get(0).getProduct().getFarm();
        FarmerVerification v = verifications.findByFarmerId(f.getOwner().getId()).orElse(null);
        String place = f.getName() + (f.getSubCounty() == null || f.getSubCounty().isBlank() ? "" : ", " + f.getSubCounty());
        return new Pickup(f.getOwner(), place, f.getSubCounty(), v == null ? null : v.getFarmLat(), v == null ? null : v.getFarmLng());
    }

    private Quote quote(Pickup p, Double dropLat, Double dropLng, DeliverySettings s) {
        Double km = null;
        if (DeliveryFees.validKenyaPoint(p.lat(), p.lng()) && DeliveryFees.validKenyaPoint(dropLat, dropLng))
            km = DeliveryFees.roadKm(p.lat(), p.lng(), dropLat, dropLng, s.getRoadFactor());
        return new Quote(km, DeliveryFees.fee(s.getBaseFee(), s.getPerKmRate(), km), s.getBaseFee(), s.getPerKmRate(), p.place(), km != null);
    }

    private JobView view(DeliveryJob j, User me) {
        boolean transporter = me.getRole() == Role.COMPANY;
        boolean admin = me.getRole() == Role.ADMIN;
        boolean open = !DeliveryJob.REQUESTED.equals(j.getStatus()) && !DeliveryJob.DECLINED.equals(j.getStatus()) && !DeliveryJob.CANCELLED.equals(j.getStatus());
        // a transporter only gets the buyer's and the farmer's phone numbers once they have taken the job
        boolean showPhones = admin || !transporter || open;
        Company c = j.getTransporter();
        return new JobView(j.getId(), j.getOrder().getId(), j.getStatus(), j.getBuyer().getName(),
                showPhones ? j.getOrder().getDeliveryPhone() : null, j.getFarmer().getName(), showPhones ? j.getFarmer().getPhone() : null,
                c.getId(), c.getName(), c.getRepPhone(), j.getPickupPlace(), j.getDropAddress(), j.getDistanceKm(), j.getFee(),
                j.getBaseFee(), j.getPerKmRate(), j.isFeePaid(), j.getNote(), j.getDeclineReason(),
                j.getDistanceKm() == null && DeliveryJob.REQUESTED.equals(j.getStatus()),
                j.getCreatedAt(), j.getAcceptedAt(), j.getPickedUpAt(), j.getDeliveredAt(), j.getFeePaidAt());
    }
}

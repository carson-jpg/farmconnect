package com.farmconnect.controller;

import com.farmconnect.dto.Trust.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Daily market price board. County officers post prices; everyone compares them and can subscribe to crop alerts. */
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {
    private final MarketPriceRepository prices;
    private final PriceAlertRepository alerts;
    private final NotificationService notifier;

    /** Latest price for every crop + market + unit, with the previous price and the change in percent. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<PriceResponse> latest(@RequestParam(required = false) String q, @AuthenticationPrincipal User me) {
        return latestFor(q, me == null ? Set.of() : subscribed(me.getId()));
    }

    public List<PriceResponse> latestFor(String q, Set<String> mySubs) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        Map<String, List<MarketPrice>> byKey = new LinkedHashMap<>();
        for (MarketPrice p : prices.findAllByOrderByPriceDateDescIdDesc()) {   // newest first
            byKey.computeIfAbsent(key(p), k -> new ArrayList<>()).add(p);
        }
        List<PriceResponse> out = new ArrayList<>();
        for (List<MarketPrice> list : byKey.values()) {
            MarketPrice cur = list.get(0);
            if (!needle.isEmpty() && !cur.getCrop().toLowerCase().contains(needle) && !cur.getMarket().toLowerCase().contains(needle)) continue;
            MarketPrice prev = list.stream().filter(x -> x.getPriceDate().isBefore(cur.getPriceDate())).findFirst().orElse(null);
            Double pct = null;
            if (prev != null && prev.getPrice().signum() > 0) {
                pct = cur.getPrice().subtract(prev.getPrice()).multiply(BigDecimal.valueOf(100))
                        .divide(prev.getPrice(), 1, RoundingMode.HALF_UP).doubleValue();
            }
            out.add(new PriceResponse(cur.getId(), cur.getCrop(), cur.getMarket(), cur.getUnit(), cur.getPrice(), cur.getPriceDate(),
                    prev == null ? null : prev.getPrice(), pct, cur.getPostedBy().getName(), mySubs.contains(cur.getCrop().toLowerCase())));
        }
        out.sort(Comparator.comparing((PriceResponse r) -> r.crop().toLowerCase()).thenComparing(PriceResponse::price));
        return out;
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<PricePoint> history(@RequestParam String crop, @RequestParam(required = false) String market,
                                    @RequestParam(defaultValue = "30") int limit) {
        return prices.findAllByOrderByPriceDateDescIdDesc().stream()
                .filter(p -> p.getCrop().equalsIgnoreCase(crop))
                .filter(p -> market == null || market.isBlank() || p.getMarket().equalsIgnoreCase(market))
                .limit(Math.max(1, Math.min(limit, 90)))
                .map(p -> new PricePoint(p.getId(), p.getPriceDate(), p.getPrice(), p.getUnit(), p.getMarket())).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public PriceResponse post(@Valid @RequestBody PriceRequest r, @AuthenticationPrincipal User u) {
        LocalDate date = r.date() == null ? LocalDate.now() : r.date();
        if (date.isAfter(LocalDate.now().plusDays(1)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The price date cannot be in the future");
        MarketPrice p = new MarketPrice();
        p.setCrop(pretty(r.crop()));
        p.setMarket(pretty(r.market()));
        p.setUnit(r.unit().trim());
        p.setPrice(r.price());
        p.setPriceDate(date);
        p.setPostedBy(u);
        p = prices.save(p);
        String msg = p.getMarket() + ": KES " + p.getPrice().stripTrailingZeros().toPlainString() + " per " + p.getUnit();
        for (PriceAlert a : alerts.findByCropIgnoreCase(p.getCrop())) {
            notifier.notifyAndSms(a.getUser(), "PRICE", p.getCrop() + " price update", msg, p.getId());
        }
        final MarketPrice saved = p;
        MarketPrice prev = prices.findAllByOrderByPriceDateDescIdDesc().stream()
                .filter(x -> key(x).equals(key(saved)) && x.getPriceDate().isBefore(saved.getPriceDate())).findFirst().orElse(null);
        Double pct = prev == null || prev.getPrice().signum() <= 0 ? null
                : saved.getPrice().subtract(prev.getPrice()).multiply(BigDecimal.valueOf(100)).divide(prev.getPrice(), 1, RoundingMode.HALF_UP).doubleValue();
        return new PriceResponse(saved.getId(), saved.getCrop(), saved.getMarket(), saved.getUnit(), saved.getPrice(), saved.getPriceDate(),
                prev == null ? null : prev.getPrice(), pct, u.getName(), false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public void delete(@PathVariable Long id) {
        if (!prices.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Price not found");
        prices.deleteById(id);
    }

    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public List<String> myAlerts(@AuthenticationPrincipal User me) {
        return alerts.findByUserId(me.getId()).stream().map(PriceAlert::getCrop).sorted().toList();
    }

    @PostMapping("/alerts/toggle")
    @Transactional
    public AlertToggle toggle(@RequestParam String crop, @AuthenticationPrincipal User me) {
        Optional<PriceAlert> ex = alerts.findByUserIdAndCropIgnoreCase(me.getId(), crop.trim());
        if (ex.isPresent()) {
            alerts.delete(ex.get());
            return new AlertToggle(ex.get().getCrop(), false);
        }
        PriceAlert a = new PriceAlert();
        a.setUser(me);
        a.setCrop(pretty(crop));
        alerts.save(a);
        return new AlertToggle(a.getCrop(), true);
    }

    private Set<String> subscribed(Long userId) {
        return alerts.findByUserId(userId).stream().map(a -> a.getCrop().toLowerCase()).collect(Collectors.toSet());
    }

    private static String key(MarketPrice p) {
        return p.getCrop().toLowerCase() + "|" + p.getMarket().toLowerCase() + "|" + p.getUnit().toLowerCase();
    }

    /** "maize  " -> "Maize", "kitale town" -> "Kitale Town" */
    static String pretty(String s) {
        String[] words = s.trim().replaceAll("\\s+", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}

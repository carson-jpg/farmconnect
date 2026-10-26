package com.farmconnect.controller;

import com.farmconnect.dto.Trust.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.NotificationService;
import com.farmconnect.service.PriceTrend;
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
        return latestFor(q, me == null ? Map.of() : mine(me.getId()));
    }

    /** @param mySubs the caller's alerts keyed by lower-case crop name */
    public List<PriceResponse> latestFor(String q, Map<String, PriceAlert> mySubs) {
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
            PriceAlert myAlert = mySubs.get(cur.getCrop().toLowerCase());
            out.add(new PriceResponse(cur.getId(), cur.getCrop(), cur.getMarket(), cur.getUnit(), cur.getPrice(), cur.getPriceDate(),
                    prev == null ? null : prev.getPrice(), pct, cur.getPostedBy().getName(), myAlert != null,
                    myAlert == null ? null : myAlert.getTargetPrice(), myAlert == null ? null : myAlert.getDirection()));
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

    /**
     * Trend and a simple forecast for one crop (optionally one market) over the last {@code days} days.
     * With no market, prices from every market in the crop's most recent unit are averaged per day.
     */
    @GetMapping("/trend")
    @Transactional(readOnly = true)
    public TrendResponse trend(@RequestParam String crop, @RequestParam(required = false) String market,
                               @RequestParam(defaultValue = "60") int days) {
        LocalDate since = LocalDate.now().minusDays(Math.max(7, Math.min(days, 365)));
        List<MarketPrice> all = prices.findAllByOrderByPriceDateDescIdDesc().stream()
                .filter(p -> p.getCrop().equalsIgnoreCase(crop.trim()))
                .filter(p -> market == null || market.isBlank() || p.getMarket().equalsIgnoreCase(market.trim()))
                .toList();
        String unit = all.isEmpty() ? "" : all.get(0).getUnit();            // newest first: keep one unit so prices compare
        List<PriceTrend.Point> pts = all.stream().filter(p -> p.getUnit().equalsIgnoreCase(unit))
                .filter(p -> !p.getPriceDate().isBefore(since))
                .map(p -> new PriceTrend.Point(p.getPriceDate(), p.getPrice().doubleValue())).toList();
        String name = all.isEmpty() ? pretty(crop) : all.get(0).getCrop();
        PriceTrend.Result r = PriceTrend.compute(name, unit, pts);
        return new TrendResponse(name, market == null || market.isBlank() ? null : pretty(market), unit, r.direction(), r.changePct(), r.low(), r.high(),
                r.average(), r.history().stream().map(x -> new TrendPoint(x.date().toString(), x.price())).toList(),
                r.forecast().stream().map(x -> new TrendPoint(x.date().toString(), x.price())).toList(), r.confidence(), r.summary());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN','OFFICER')")
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
        final MarketPrice saved = p;
        MarketPrice prev = prices.findAllByOrderByPriceDateDescIdDesc().stream()
                .filter(x -> key(x).equals(key(saved)) && x.getPriceDate().isBefore(saved.getPriceDate())).findFirst().orElse(null);

        String msg = p.getMarket() + ": KES " + p.getPrice().stripTrailingZeros().toPlainString() + " per " + p.getUnit();
        for (PriceAlert a : alerts.findByCropIgnoreCase(p.getCrop())) {
            if (a.getTargetPrice() == null) {                       // plain alert: every new price
                notifier.notifyAndSms(a.getUser(), "PRICE", p.getCrop() + " price update", msg, p.getId());
            } else if (reached(a, p.getPrice()) && (prev == null || !reached(a, prev.getPrice()))) {
                // target alert: only when this market first crosses the line, not on every later update
                String line = ("BELOW".equals(a.getDirection()) ? " (your target: KES " + plain(a.getTargetPrice()) + " or lower)"
                        : " (your target: KES " + plain(a.getTargetPrice()) + " or higher)");
                notifier.notifyAndSms(a.getUser(), "PRICE", p.getCrop() + " reached your target", msg + line, p.getId());
            }
        }
        Double pct = prev == null || prev.getPrice().signum() <= 0 ? null
                : saved.getPrice().subtract(prev.getPrice()).multiply(BigDecimal.valueOf(100)).divide(prev.getPrice(), 1, RoundingMode.HALF_UP).doubleValue();
        return new PriceResponse(saved.getId(), saved.getCrop(), saved.getMarket(), saved.getUnit(), saved.getPrice(), saved.getPriceDate(),
                prev == null ? null : prev.getPrice(), pct, u.getName(), false, null, null);
    }

    /** A county admin can remove prices they posted, or ones posted by staff of their own county; not other counties'. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN','OFFICER')")
    @Transactional
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User me) {
        MarketPrice p = prices.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Price not found"));
        if (me.getRole() == Role.COUNTY_ADMIN) {
            User by = p.getPostedBy();
            boolean mine = by.getId().equals(me.getId())
                    || (by.getRole() != Role.ADMIN && by.getAssignedCounty() != null && by.getAssignedCounty().equals(me.getAssignedCounty()));
            if (!mine) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This price was posted outside your county");
        }
        prices.delete(p);
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

    /** My alerts with their target prices, one per crop. */
    @GetMapping("/alerts/details")
    @Transactional(readOnly = true)
    public List<AlertInfo> myAlertDetails(@AuthenticationPrincipal User me) {
        return alerts.findByUserId(me.getId()).stream()
                .sorted(Comparator.comparing((PriceAlert a) -> a.getCrop().toLowerCase()))
                .map(a -> new AlertInfo(a.getCrop(), a.getTargetPrice(), a.getDirection())).toList();
    }

    /**
     * Set a target price for a crop: "ABOVE" = tell me when it rises to this or higher, "BELOW" = falls to this or lower.
     * Also subscribes me to the crop if I was not yet. Leave the price out (or 0) to drop the target and go back
     * to hearing about every new price.
     */
    @PostMapping("/alerts/target")
    @Transactional
    public AlertInfo setTarget(@RequestParam String crop, @RequestParam(required = false) BigDecimal price,
                               @RequestParam(defaultValue = "ABOVE") String direction, @AuthenticationPrincipal User me) {
        String dir = direction.trim().toUpperCase();
        if (!dir.equals("ABOVE") && !dir.equals("BELOW"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction must be ABOVE or BELOW");
        if (crop.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crop is required");
        PriceAlert a = alerts.findByUserIdAndCropIgnoreCase(me.getId(), crop.trim()).orElseGet(() -> {
            PriceAlert n = new PriceAlert();
            n.setUser(me);
            n.setCrop(pretty(crop));
            return n;
        });
        if (price == null || price.signum() <= 0) {
            a.setTargetPrice(null);
            a.setDirection(null);
        } else {
            a.setTargetPrice(price.setScale(2, RoundingMode.HALF_UP));
            a.setDirection(dir);
        }
        a = alerts.save(a);
        return new AlertInfo(a.getCrop(), a.getTargetPrice(), a.getDirection());
    }

    private Map<String, PriceAlert> mine(Long userId) {
        return alerts.findByUserId(userId).stream()
                .collect(Collectors.toMap(a -> a.getCrop().toLowerCase(), a -> a, (x, y) -> x));
    }

    /** True when {@code price} is on the target side of the alert's line (an alert without a target is always "reached"). */
    static boolean reached(PriceAlert a, BigDecimal price) {
        if (a.getTargetPrice() == null) return true;
        int cmp = price.compareTo(a.getTargetPrice());
        return "BELOW".equals(a.getDirection()) ? cmp <= 0 : cmp >= 0;
    }

    private static String plain(BigDecimal v) { return v.stripTrailingZeros().toPlainString(); }

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

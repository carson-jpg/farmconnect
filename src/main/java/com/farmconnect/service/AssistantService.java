package com.farmconnect.service;

import com.farmconnect.dto.Community.WeatherDay;
import com.farmconnect.dto.Community.WeatherResponse;
import com.farmconnect.controller.WeatherController;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The brain behind the feature-phone channels: USSD menus and SMS keywords.
 * It only reads public information (prices, weather, tips, notices) plus the caller's own orders, matched by phone number.
 * Everything exists in Swahili and English.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {
    /** USSD screens must stay under 182 characters. */
    private static final int USSD_MAX = 180;
    private static final Set<PostType> TIPS = Set.of(PostType.TIP, PostType.ADVISORY, PostType.ARTICLE);
    private static final Set<PostType> NOTICES = Set.of(PostType.ANNOUNCEMENT, PostType.PROGRAMME, PostType.TRAINING, PostType.OPPORTUNITY);

    private final MarketPriceRepository prices;
    private final PostRepository posts;
    private final UserRepository users;
    private final OrderRepository orders;
    private final WeatherController weather;

    @Value("${app.sms.shortcode:}") private String shortcode;

    // ======================================================== USSD

    /** @param text everything the user typed so far, joined with '*' (Africa's Talking convention) */
    @Transactional(readOnly = true)
    public String ussd(String phone, String text) {
        String[] in = text == null || text.isBlank() ? new String[0] : text.split("\\*");
        if (in.length == 0) return "CON Karibu FarmConnect / Welcome\n1. Kiswahili\n2. English";
        boolean sw;
        if (in[0].equals("1")) sw = true;
        else if (in[0].equals("2")) sw = false;
        else return "END Chaguo si sahihi / Invalid choice";

        if (in.length == 1) {
            return "CON " + (sw ? "FarmConnect\n1. Bei za soko\n2. Hali ya hewa\n3. Vidokezo vya kilimo\n4. Oda zangu\n5. Matangazo ya kaunti\n6. Msaada\n0. Toka"
                    : "FarmConnect\n1. Market prices\n2. Weather\n3. Farming tips\n4. My orders\n5. County notices\n6. Help\n0. Exit");
        }
        switch (in[1]) {
            case "1": return ussdPrices(in, sw);
            case "2": return "END " + fit(weatherSummary(sw), USSD_MAX);
            case "3": return ussdTips(in, sw);
            case "4": return "END " + fit(ordersSummary(phone, sw), USSD_MAX);
            case "5": return "END " + fit(noticesSummary(sw), USSD_MAX);
            case "6": return "END " + fit(helpText(sw), USSD_MAX);
            case "0": return "END " + (sw ? "Asante kwa kutumia FarmConnect." : "Thank you for using FarmConnect.");
            default: return "END " + (sw ? "Chaguo si sahihi." : "Invalid choice.");
        }
    }

    private String ussdPrices(String[] in, boolean sw) {
        List<String> crops = crops();
        if (crops.isEmpty()) return "END " + (sw ? "Bei bado hazijawekwa." : "No prices have been posted yet.");
        if (in.length == 2) {
            StringBuilder sb = new StringBuilder("CON " + (sw ? "Chagua zao:" : "Choose a crop:"));
            for (int i = 0; i < crops.size(); i++) sb.append('\n').append(i + 1).append(". ").append(crops.get(i));
            return fit(sb.toString(), USSD_MAX + 4);
        }
        int idx = parse(in[2]) - 1;
        if (idx < 0 || idx >= crops.size()) return "END " + (sw ? "Chaguo si sahihi." : "Invalid choice.");
        return "END " + fit(priceLines(crops.get(idx), sw), USSD_MAX);
    }

    private String ussdTips(String[] in, boolean sw) {
        List<Post> tips = posts.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> TIPS.contains(p.getType()) && !"BUYERS".equals(p.getAudience())).limit(5).toList();
        if (tips.isEmpty()) return "END " + (sw ? "Hakuna vidokezo kwa sasa." : "No tips available yet.");
        if (in.length == 2) {
            StringBuilder sb = new StringBuilder("CON " + (sw ? "Vidokezo:" : "Tips:"));
            for (int i = 0; i < tips.size(); i++) sb.append('\n').append(i + 1).append(". ").append(fit(tips.get(i).getTitle(), 28));
            return fit(sb.toString(), USSD_MAX + 4);
        }
        int idx = parse(in[2]) - 1;
        if (idx < 0 || idx >= tips.size()) return "END " + (sw ? "Chaguo si sahihi." : "Invalid choice.");
        Post p = tips.get(idx);
        return "END " + fit(p.getTitle() + ": " + p.getBody().replaceAll("\\s+", " "), USSD_MAX);
    }

    // ======================================================== SMS keywords

    /** @return the reply to send back, or null if nothing should be sent */
    @Transactional(readOnly = true)
    public String sms(String from, String raw) {
        if (raw == null || raw.isBlank()) return helpText(true) + " / " + helpText(false);
        String[] t = raw.trim().replaceAll("\\s+", " ").split(" ", 2);
        String cmd = t[0].toUpperCase(Locale.ROOT);
        String arg = t.length > 1 ? t[1].trim() : "";
        switch (cmd) {
            case "BEI": case "BEI:": return smsPrices(arg, true);
            case "PRICE": case "PRICES": return smsPrices(arg, false);
            case "HALI": return weatherSummary(true);
            case "WEATHER": return weatherSummary(false);
            case "ODA": case "ODAZANGU": return ordersSummary(from, true);
            case "ORDER": case "ORDERS": return ordersSummary(from, false);
            case "KIDOKEZO": case "SHAURI": case "VIDOKEZO": return latestTip(true);
            case "TIP": case "TIPS": return latestTip(false);
            case "MATANGAZO": return noticesSummary(true);
            case "NEWS": case "NOTICES": return noticesSummary(false);
            case "MSAADA": case "HELP": case "?": return helpText(cmd.equals("MSAADA")) ;
            default: return helpText(true) + "\n" + helpText(false);
        }
    }

    private String smsPrices(String crop, boolean sw) {
        if (crop.isEmpty()) {
            List<String> c = crops();
            return c.isEmpty() ? (sw ? "Bei bado hazijawekwa." : "No prices posted yet.")
                    : (sw ? "Tuma: BEI <zao>. Mazao: " : "Send: PRICE <crop>. Crops: ") + String.join(", ", c);
        }
        String match = crops().stream().filter(c -> c.equalsIgnoreCase(crop) || c.toLowerCase().startsWith(crop.toLowerCase()))
                .findFirst().orElse(null);
        if (match == null) return (sw ? "Hatuna bei ya \"" : "No price found for \"") + crop + "\". " + (sw ? "Tuma BEI kuona mazao." : "Send PRICE to see crops.");
        return fit(priceLines(match, sw), 300);
    }

    // ======================================================== shared pieces

    private List<String> crops() {
        return prices.findAllByOrderByPriceDateDescIdDesc().stream().map(MarketPrice::getCrop)
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(8).toList();
    }

    /** Latest price per market for one crop: "Maize: Kitale 3200/90kg bag, Endebess 3100/90kg bag" */
    private String priceLines(String crop, boolean sw) {
        Map<String, MarketPrice> latest = new LinkedHashMap<>();
        for (MarketPrice p : prices.findAllByOrderByPriceDateDescIdDesc()) {
            if (!p.getCrop().equalsIgnoreCase(crop)) continue;
            latest.putIfAbsent(p.getMarket().toLowerCase() + "|" + p.getUnit().toLowerCase(), p);
        }
        if (latest.isEmpty()) return sw ? "Hakuna bei ya " + crop : "No price for " + crop;
        LocalDate newest = latest.values().stream().map(MarketPrice::getPriceDate).max(Comparator.naturalOrder()).orElse(LocalDate.now());
        StringBuilder sb = new StringBuilder(crop + " (" + newest.format(DateTimeFormatter.ofPattern("d MMM")) + ")");
        latest.values().stream().sorted(Comparator.comparing(MarketPrice::getPrice)).limit(6)
                .forEach(p -> sb.append('\n').append(p.getMarket()).append(' ').append(money(p.getPrice())).append('/').append(p.getUnit()));
        return sb.toString();
    }

    private String weatherSummary(boolean sw) {
        try {
            WeatherResponse w = weather.weather(1.0191, 35.0020, "Kitale");
            StringBuilder sb = new StringBuilder((sw ? "Kitale sasa " : "Kitale now ") + Math.round(w.tempC()) + "C.");
            for (int i = 0; i < Math.min(3, w.days().size()); i++) {
                WeatherDay d = w.days().get(i);
                String label = i == 0 ? (sw ? "Leo" : "Today") : i == 1 ? (sw ? "Kesho" : "Tmrw") : (sw ? "Kesho kutwa" : "Day 3");
                sb.append('\n').append(label).append(' ').append(Math.round(d.minC())).append('-').append(Math.round(d.maxC()))
                        .append("C ").append(sw ? "mvua " : "rain ").append(d.rainProb()).append('%');
            }
            return sb.toString();
        } catch (Exception e) {
            return sw ? "Hali ya hewa haipatikani sasa. Jaribu baadaye." : "Weather is not available right now. Try again later.";
        }
    }

    private String latestTip(boolean sw) {
        return posts.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> TIPS.contains(p.getType()) && !"BUYERS".equals(p.getAudience())).findFirst()
                .map(p -> fit(p.getTitle() + ": " + p.getBody().replaceAll("\\s+", " "), 300))
                .orElse(sw ? "Hakuna vidokezo kwa sasa." : "No tips available yet.");
    }

    private String noticesSummary(boolean sw) {
        List<Post> list = posts.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> NOTICES.contains(p.getType()) && !"BUYERS".equals(p.getAudience())).limit(3).toList();
        if (list.isEmpty()) return sw ? "Hakuna matangazo kwa sasa." : "No notices right now.";
        return list.stream().map(p -> "- " + p.getTitle() + (p.getEventDate() == null ? ""
                        : " (" + p.getEventDate().toString().substring(0, 10) + ")"))
                .collect(Collectors.joining("\n"));
    }

    private String ordersSummary(String phone, boolean sw) {
        User u = findByPhone(phone);
        if (u == null) return sw ? "Namba hii haijasajiliwa. Pakua programu ya FarmConnect ujisajili."
                : "This number is not registered. Install the FarmConnect app to sign up.";
        List<CustomerOrder> list = u.getRole() == Role.FARMER ? orders.findForFarmer(u.getId())
                : orders.findByBuyerIdOrderByCreatedAtDesc(u.getId());
        if (list.isEmpty()) return sw ? "Huna oda bado." : "You have no orders yet.";
        return list.stream().limit(3).map(o -> "#" + o.getId() + " " + statusText(o.getStatus(), sw) + " " + money(o.getTotal()))
                .collect(Collectors.joining("\n"));
    }

    private String helpText(boolean sw) {
        String code = shortcode == null || shortcode.isBlank() ? "" : " " + (sw ? "kwa " : "to ") + shortcode;
        return sw ? "SMS" + code + ": BEI <zao>, HALI, ODA, VIDOKEZO, MATANGAZO. Pakua programu ya FarmConnect kwa huduma zaidi."
                : "SMS" + code + ": PRICE <crop>, WEATHER, ORDERS, TIPS, NEWS. Install the FarmConnect app for more.";
    }

    private static String statusText(OrderStatus s, boolean sw) {
        switch (s) {
            case PENDING: return sw ? "inasubiri" : "pending";
            case CONFIRMED: return sw ? "imethibitishwa" : "confirmed";
            case SHIPPED: return sw ? "njiani" : "on the way";
            case DELIVERED: return sw ? "imefika" : "delivered";
            default: return sw ? "imeghairiwa" : "cancelled";
        }
    }

    // ======================================================== helpers

    private User findByPhone(String phone) {
        String key = lastNine(phone);
        if (key.length() < 9) return null;
        return users.findAll().stream().filter(User::isEnabled)
                .filter(u -> u.getPhone() != null && lastNine(u.getPhone()).equals(key)).findFirst().orElse(null);
    }

    static String lastNine(String phone) {
        String d = phone == null ? "" : phone.replaceAll("\\D", "");
        return d.length() <= 9 ? d : d.substring(d.length() - 9);
    }

    private static String money(BigDecimal v) { return v.stripTrailingZeros().toPlainString(); }

    private static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private static String fit(String s, int max) { return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…"; }
}

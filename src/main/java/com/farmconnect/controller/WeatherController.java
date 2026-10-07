package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real forecast from Open-Meteo (free, no API key) with simple farming advice on top.
 * Default place is Kitale (Trans Nzoia). Results are cached for 30 minutes.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final Map<String, WeatherResponse> cache = new ConcurrentHashMap<>();

    @GetMapping
    public WeatherResponse weather(@RequestParam(defaultValue = "1.0191") double lat,
                                   @RequestParam(defaultValue = "35.0020") double lon,
                                   @RequestParam(defaultValue = "Kitale, Trans Nzoia") String place) {
        String key = Math.round(lat * 10) + ":" + Math.round(lon * 10);
        WeatherResponse cached = cache.get(key);
        if (cached != null && cached.updatedAt().isAfter(Instant.now().minusSeconds(1800))) return cached;
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max"
                    + "&timezone=Africa%2FNairobi&forecast_days=7";
            HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new IllegalStateException("status " + res.statusCode());
            JsonNode root = JSON.readTree(res.body());
            JsonNode cur = root.get("current");
            JsonNode d = root.get("daily");
            List<WeatherDay> days = new ArrayList<>();
            for (int i = 0; i < d.get("time").size(); i++) {
                days.add(new WeatherDay(d.get("time").get(i).asText(), d.get("temperature_2m_min").get(i).asDouble(),
                        d.get("temperature_2m_max").get(i).asDouble(), d.get("precipitation_sum").get(i).asDouble(),
                        d.get("precipitation_probability_max").get(i).asInt()));
            }
            WeatherResponse out = new WeatherResponse(place, cur.get("temperature_2m").asDouble(),
                    cur.get("relative_humidity_2m").asInt(), cur.get("wind_speed_10m").asDouble(),
                    cur.get("precipitation").asDouble(), days, advice(days), Instant.now());
            cache.put(key, out);
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather service interrupted");
        } catch (Exception e) {
            if (cached != null) return cached;     // stale is better than nothing
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather service is not reachable right now. Try again later.");
        }
    }

    static List<String> advice(List<WeatherDay> days) {
        List<String> a = new ArrayList<>();
        double rain3 = days.stream().limit(3).mapToDouble(WeatherDay::rainMm).sum();
        double rain7 = days.stream().mapToDouble(WeatherDay::rainMm).sum();
        int soonProb = days.stream().limit(2).mapToInt(WeatherDay::rainProb).max().orElse(0);
        double hottest = days.stream().limit(3).mapToDouble(WeatherDay::maxC).max().orElse(0);
        if (rain3 >= 25) a.add("Heavy rain expected in the next 3 days: delay spraying and top-dressing, clear drainage channels and keep harvested grain under cover.");
        else if (soonProb >= 60) a.add("Rain is likely in the next 2 days: a good moment to plant if the soil is ready. Avoid drying harvested produce outdoors.");
        if (rain7 < 5) a.add("A dry week is ahead: good for harvesting and drying maize. Irrigate young crops and vegetables where you can.");
        if (hottest >= 30) a.add("Hot days ahead: give livestock more water and shade, and water vegetables early morning or evening.");
        if (a.isEmpty()) a.add("Conditions look moderate this week. Follow your normal field plan and check the forecast again tomorrow.");
        return a;
    }
}

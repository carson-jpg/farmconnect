package com.farmconnect.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Turns a price history into a trend and a simple forecast: a straight line fitted through the recent prices.
 * It is an estimate for guidance only (it cannot know about weather, supply shocks or holidays), and says how
 * sure it is: HIGH needs plenty of points that follow the line closely, LOW means "treat this as a hint".
 */
public final class PriceTrend {
    private PriceTrend() {}

    public record Point(LocalDate date, double price) {}
    public record Result(String direction, double changePct, double low, double high, double average,
                         List<Point> history, List<Point> forecast, String confidence, String summary) {}

    /** @param raw any number of prices (several per day are averaged); @param crop used in the sentence only */
    public static Result compute(String crop, String unit, List<Point> raw) {
        TreeMap<LocalDate, double[]> byDay = new TreeMap<>();
        for (Point p : raw) {
            double[] s = byDay.computeIfAbsent(p.date(), d -> new double[2]);
            s[0] += p.price();
            s[1] += 1;
        }
        List<Point> h = new ArrayList<>();
        byDay.forEach((d, s) -> h.add(new Point(d, round2(s[0] / s[1]))));
        int n = h.size();
        if (n < 3) {
            return new Result("NOT_ENOUGH_DATA", 0, n == 0 ? 0 : min(h), n == 0 ? 0 : max(h), n == 0 ? 0 : avg(h), h, List.of(), "LOW",
                    "Not enough prices yet to show a trend for " + crop + ". Check again after a few more updates.");
        }
        LocalDate first = h.get(0).date();
        double[] x = new double[n], y = new double[n];
        for (int i = 0; i < n; i++) { x[i] = ChronoUnit.DAYS.between(first, h.get(i).date()); y[i] = h.get(i).price(); }
        double mx = mean(x), my = mean(y), sxx = 0, sxy = 0, syy = 0;
        for (int i = 0; i < n; i++) { sxx += (x[i] - mx) * (x[i] - mx); sxy += (x[i] - mx) * (y[i] - my); syy += (y[i] - my) * (y[i] - my); }
        double slope = sxx == 0 ? 0 : sxy / sxx;
        double intercept = my - slope * mx;
        double r2 = (sxx == 0 || syy == 0) ? 0 : (sxy * sxy) / (sxx * syy);

        double span = x[n - 1] - x[0];
        double fitFirst = Math.max(0.01, intercept + slope * x[0]);
        double fitLast = intercept + slope * x[n - 1];
        double pct = span <= 0 ? 0 : (fitLast - fitFirst) / fitFirst * 100.0;
        pct = round1(pct);
        String dir = pct > 3 ? "RISING" : pct < -3 ? "FALLING" : "STABLE";

        // forecast: continue the line, but only half as steeply (prices rarely run in a straight line for long)
        List<Point> f = new ArrayList<>();
        double lastX = x[n - 1];
        double base = fitLast;
        for (int ahead : new int[]{7, 14}) {
            double v = Math.max(0, base + slope * 0.5 * ahead);
            f.add(new Point(h.get(n - 1).date().plusDays(ahead), round2(v)));
        }
        String conf = (n >= 10 && r2 >= 0.5) ? "HIGH" : (n >= 5 && r2 >= 0.3) ? "MEDIUM" : "LOW";
        String perUnit = unit == null || unit.isBlank() ? "" : " per " + unit;
        String what = dir.equals("RISING") ? "rising" : dir.equals("FALLING") ? "falling" : "steady";
        StringBuilder sb = new StringBuilder(crop).append(" prices are ").append(what);
        if (!dir.equals("STABLE")) sb.append(" (about ").append(String.format(Locale.US, "%.1f", Math.abs(pct))).append("% over the last ").append((int) span).append(" days)");
        sb.append(". Estimate in 7 days: KES ").append(Math.round(f.get(0).price())).append(perUnit)
          .append(" (").append(conf.toLowerCase()).append(" confidence). This is a guide, not a promise.");
        return new Result(dir, pct, min(h), max(h), round2(avg(h)), h, f, conf, sb.toString());
    }

    private static double mean(double[] a) { double s = 0; for (double v : a) s += v; return s / a.length; }
    private static double min(List<Point> l) { return l.stream().mapToDouble(Point::price).min().orElse(0); }
    private static double max(List<Point> l) { return l.stream().mapToDouble(Point::price).max().orElse(0); }
    private static double avg(List<Point> l) { return l.stream().mapToDouble(Point::price).average().orElse(0); }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}

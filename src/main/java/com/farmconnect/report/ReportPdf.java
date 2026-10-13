package com.farmconnect.report;

import com.farmconnect.dto.Community.Analytics;
import com.farmconnect.dto.Community.Indicators;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The county report as a PDF: programme indicators, bar charts and a sub-county table. */
public final class ReportPdf {
    private static final int GREEN = 0x2E7D32, DARK = 0x1B5E20, LIGHT = 0xC8E6C9, INK = 0x1C2B1E, GREY = 0x5E7061,
            PALE = 0xF1F6EF, AMBER = 0xF9A825, RULE = 0xD5E3D0, WHITE = 0xFFFFFF;
    private static final float M = 40, TOP = SimplePdf.H - 40, BOTTOM = 52, CONTENT_W = SimplePdf.W - 2 * M;

    private final SimplePdf pdf = new SimplePdf("FarmConnect county report");
    private float y;

    private ReportPdf() {}

    public static byte[] build(Analytics a, String generatedBy) {
        ReportPdf r = new ReportPdf();
        r.compose(a, generatedBy == null ? "" : generatedBy);
        return r.pdf.toBytes();
    }

    private void compose(Analytics a, String by) {
        header(by, a.scopeLabel());
        indicators(a.indicators(), a.scoped());
        if (!a.scoped() && a.farmersByCounty().size() > 1) pairChart("Farmers by county", a.farmersByCounty(), a.verifiedByCounty());
        pairChart("Farmers by sub-county", a.farmersBySubCounty(), a.verifiedBySubCounty());
        bars("Live products by category", a.productsByCategory(), GREEN, false, false, "");
        bars("Orders per month", a.ordersByMonth(), GREEN, true, false, "");
        bars("Sales per month (KES)", toLong(a.salesByMonth()), AMBER, true, false, "KES ");
        bars("Recorded harvest by crop (kg)", a.harvestKgByCrop(), GREEN, false, false, "");
        bars("Groups by type", a.groupsByType(), GREEN, false, false, "");
        bars("Largest groups (members)", a.membersByGroup(), GREEN, false, true, "");
        bars("Information published by type", a.postsByType(), GREEN, false, false, "");
        subCountyTable(a);
        int pages = pdf.pageCount();
        for (int i = 0; i < pages; i++) {
            pdf.page(i);
            pdf.line(M, 36, SimplePdf.W - M, 36, 0.5f, RULE);
            pdf.text(M, 24, 8, false, GREY, "FarmConnect county report");
            String p = "Page " + (i + 1) + " of " + pages;
            pdf.text(SimplePdf.W - M - SimplePdf.width(p, 8, false), 24, 8, false, GREY, p);
        }
    }

    private void header(String by, String scopeLabel) {
        pdf.rect(0, SimplePdf.H - 92, SimplePdf.W, 92, GREEN);
        pdf.rect(0, SimplePdf.H - 96, SimplePdf.W, 4, AMBER);
        pdf.text(M, SimplePdf.H - 46, 24, true, WHITE, "FarmConnect");
        pdf.text(M, SimplePdf.H - 66, 11, false, 0xE8F5E9, "County report - " + (scopeLabel == null || scopeLabel.isBlank() ? "All counties" : scopeLabel));
        String when = ZonedDateTime.now(ZoneId.of("Africa/Nairobi")).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US));
        String right = "Generated " + when;
        pdf.text(SimplePdf.W - M - SimplePdf.width(right, 9, false), SimplePdf.H - 46, 9, false, WHITE, right);
        if (!by.isBlank()) {
            String b = "by " + by;
            pdf.text(SimplePdf.W - M - SimplePdf.width(b, 9, false), SimplePdf.H - 60, 9, false, WHITE, b);
        }
        y = SimplePdf.H - 96 - 28;
    }

    private void ensure(float height) {
        if (y - height >= BOTTOM) return;
        pdf.newPage();
        y = TOP;
    }

    private void heading(String text) {
        ensure(40);
        pdf.text(M, y, 13, true, DARK, text);
        pdf.line(M, y - 5, M + CONTENT_W, y - 5, 1f, GREEN);
        y -= 24;
    }

    private void indicators(Indicators i, boolean scoped) {
        heading(scoped ? "Indicators for your area" : "Programme indicators");
        String[][] all = {
                {"Registered farmers", fmt(i.registeredFarmers())}, {"Verified farmers", fmt(i.verifiedFarmers())},
                {"Registered buyers", fmt(i.registeredBuyers())}, {"Active users (30 days)", fmt(i.activeUsers30d())},
                {"Groups onboarded", fmt(i.groupsOnboarded())}, {"Group members", fmt(i.groupMembers())},
                {"Information interactions", fmt(i.informationInteractions())}, {"Opportunities accessed", fmt(i.opportunitiesAccessed())},
                {"Market listings", fmt(i.marketListings())}, {"Farmer-buyer connections", fmt(i.farmerBuyerConnections())},
                {"Officers active (30 days)", i.officersActive30d() + " of " + i.officersTotal()}, {"Programmes communicated", fmt(i.programmesCommunicated())},
                {"Farmers using digital records", fmt(i.farmersUsingRecords())}, {"Farm records created", fmt(i.recordsTotal())},
                {"Farmer satisfaction (of 5)", i.satisfactionResponses() == 0 ? "-" : String.format(Locale.US, "%.1f  (%d votes)", i.satisfactionAverage(), i.satisfactionResponses())},
                {"Messages exchanged", fmt(i.messagesSent())}};
        // an officer's report only keeps the figures that can be worked out for their own area
        java.util.Set<String> areaOnly = java.util.Set.of("Registered farmers", "Verified farmers", "Active users (30 days)", "Groups onboarded",
                "Group members", "Market listings", "Farmer-buyer connections", "Farmers using digital records", "Farm records created");
        List<String[]> keep = new ArrayList<>();
        for (String[] it : all) {
            if (scoped && !areaOnly.contains(it[0])) continue;
            keep.add(scoped && it[0].equals("Active users (30 days)") ? new String[]{"Active farmers (30 days)", it[1]} : it);
        }
        String[][] items = keep.toArray(new String[0][]);
        float gap = 8, cw = (CONTENT_W - 2 * gap) / 3, ch = 46;
        for (int k = 0; k < items.length; k++) {
            int col = k % 3;
            if (col == 0) ensure(ch + gap);
            float x = M + col * (cw + gap);
            pdf.rect(x, y - ch + 10, cw, ch, PALE);
            pdf.rect(x, y - ch + 10, 3, ch, GREEN);
            pdf.text(x + 12, y - 6, 8, false, GREY, SimplePdf.fit(items[k][0], 8, false, cw - 18));
            pdf.text(x + 12, y - 28, 16, true, INK, SimplePdf.fit(items[k][1], 16, true, cw - 18));
            if (col == 2 || k == items.length - 1) y -= ch + gap;
        }
        y -= 10;
    }

    private void pairChart(String title, Map<String, Long> registered, Map<String, Long> verified) {
        Map<String, Long> reg = top(registered, 12, false);
        if (reg.isEmpty()) return;
        float rowH = 15;
        ensure(40 + reg.size() * rowH + 20);
        heading(title);
        long max = max(reg);
        float labelW = 130, barX = M + labelW + 8, barW = CONTENT_W - labelW - 70;
        for (Map.Entry<String, Long> e : reg.entrySet()) {
            long ver = verified.getOrDefault(e.getKey(), 0L);
            pdf.text(M, y - 8, 9, false, INK, SimplePdf.fit(e.getKey(), 9, false, labelW));
            float w = max == 0 ? 0 : barW * e.getValue() / max;
            pdf.rect(barX, y - 11, Math.max(w, 1), 11, LIGHT);
            float vw = max == 0 ? 0 : barW * ver / max;
            if (vw > 0) pdf.rect(barX, y - 11, Math.max(vw, 1), 11, GREEN);
            pdf.text(barX + Math.max(w, 1) + 6, y - 8.5f, 9, true, INK, e.getValue() + "  (" + ver + " verified)");
            y -= rowH;
        }
        pdf.rect(M, y - 8, 9, 9, LIGHT);
        pdf.text(M + 14, y - 6, 8, false, GREY, "Registered");
        pdf.rect(M + 70, y - 8, 9, 9, GREEN);
        pdf.text(M + 84, y - 6, 8, false, GREY, "Verified");
        y -= 28;
    }

    private void bars(String title, Map<String, Long> data, int color, boolean keepOrder, boolean already, String prefix) {
        Map<String, Long> rows = keepOrder ? data : top(data, 10, already);
        float rowH = 15;
        ensure(40 + Math.max(rows.size(), 1) * rowH + 14);
        heading(title);
        if (rows.isEmpty() || max(rows) == 0 && !keepOrder) {
            pdf.text(M, y - 8, 9, false, GREY, "No data yet");
            y -= 28;
            return;
        }
        long max = max(rows);
        float labelW = 130, barX = M + labelW + 8, barW = CONTENT_W - labelW - 90;
        for (Map.Entry<String, Long> e : rows.entrySet()) {
            pdf.text(M, y - 8, 9, false, INK, SimplePdf.fit(e.getKey(), 9, false, labelW));
            float w = max == 0 ? 0 : barW * e.getValue() / max;
            pdf.rect(barX, y - 11, Math.max(w, e.getValue() > 0 ? 1 : 0), 11, color);
            pdf.text(barX + Math.max(w, 1) + 6, y - 8.5f, 9, true, INK, prefix + fmt(e.getValue()));
            y -= rowH;
        }
        y -= 14;
    }

    private void subCountyTable(Analytics a) {
        Map<String, Long> reg = a.farmersBySubCounty();
        if (reg.isEmpty()) return;
        float rowH = 17;
        ensure(60 + rowH * (reg.size() + 2));      // keep the whole table together
        heading("Verification by sub-county");
        float[] x = {M + 6, M + 230, M + 330, M + 430};
        pdf.rect(M, y - 13, CONTENT_W, 17, GREEN);
        String[] head = {"Sub-county", "Registered", "Verified", "% verified"};
        for (int i = 0; i < head.length; i++) pdf.text(x[i], y - 8, 9, true, WHITE, head[i]);
        y -= rowH + 4;
        int n = 0;
        long tr = 0, tv = 0;
        for (Map.Entry<String, Long> e : reg.entrySet()) {
            long v = a.verifiedBySubCounty().getOrDefault(e.getKey(), 0L);
            tr += e.getValue(); tv += v;
            if (y - rowH < BOTTOM) { pdf.newPage(); y = TOP; }
            if (n++ % 2 == 0) pdf.rect(M, y - 13, CONTENT_W, rowH, PALE);
            pdf.text(x[0], y - 8, 9, false, INK, SimplePdf.fit(e.getKey(), 9, false, 210));
            pdf.text(x[1], y - 8, 9, false, INK, String.valueOf(e.getValue()));
            pdf.text(x[2], y - 8, 9, false, INK, String.valueOf(v));
            pdf.text(x[3], y - 8, 9, false, INK, pct(v, e.getValue()));
            y -= rowH;
        }
        if (y - rowH < BOTTOM) { pdf.newPage(); y = TOP; }
        pdf.line(M, y - 13 + rowH, M + CONTENT_W, y - 13 + rowH, 1f, GREEN);
        pdf.text(x[0], y - 8, 9, true, INK, "Total");
        pdf.text(x[1], y - 8, 9, true, INK, String.valueOf(tr));
        pdf.text(x[2], y - 8, 9, true, INK, String.valueOf(tv));
        pdf.text(x[3], y - 8, 9, true, INK, pct(tv, tr));
        y -= rowH + 10;
    }

    // ---- helpers ----------------------------------------------------------------------------------------------

    private static Map<String, Long> toLong(Map<String, BigDecimal> m) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (m != null) m.forEach((k, v) -> out.put(k, v == null ? 0L : v.longValue()));
        return out;
    }

    /** The biggest entries first, at most n of them. */
    private static Map<String, Long> top(Map<String, Long> m, int n, boolean alreadySorted) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (m == null) return out;
        List<Map.Entry<String, Long>> list = new ArrayList<>(m.entrySet());
        if (!alreadySorted) list.sort((p, q) -> Long.compare(q.getValue(), p.getValue()));
        for (int i = 0; i < Math.min(n, list.size()); i++) out.put(list.get(i).getKey(), list.get(i).getValue());
        return out;
    }

    private static long max(Map<String, Long> m) {
        long mx = 0;
        for (long v : m.values()) mx = Math.max(mx, v);
        return mx;
    }

    private static String fmt(long v) { return String.format(Locale.US, "%,d", v); }

    private static String pct(long part, long whole) {
        return whole == 0 ? "-" : String.format(Locale.US, "%.0f%%", part * 100.0 / whole);
    }
}

package com.farmconnect.app.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Small helpers that turn lists from the server into the series the charts draw. */
final class Insights {
    private Insights() {}

    /** The last {@code n} months as "yyyy-MM" keys with 0 values, oldest first. */
    static LinkedHashMap<String, Double> months(int n) {
        LinkedHashMap<String, Double> m = new LinkedHashMap<>();
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.DAY_OF_MONTH, 1);
        c.add(java.util.Calendar.MONTH, -(n - 1));
        for (int i = 0; i < n; i++) {
            m.put(String.format(java.util.Locale.US, "%04d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1), 0.0);
            c.add(java.util.Calendar.MONTH, 1);
        }
        return m;
    }

    /** Adds {@code v} to the month an ISO timestamp (2026-10-02T10:00:00Z) falls in. Months outside the series are ignored. */
    static void add(Map<String, Double> months, String iso, double v) {
        if (iso == null || iso.length() < 7) return;
        String key = iso.substring(0, 7);
        Double cur = months.get(key);
        if (cur != null) months.put(key, cur + v);
    }

    /** Counts items per key, listed in the order of {@code keys}; the display name comes from {@code names}. Empty groups are kept (value 0). */
    static <T> LinkedHashMap<String, Long> countBy(List<T> list, Function<T, String> key, String[] keys, String[] names) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) out.put(names[i], 0L);
        if (list == null) return out;
        for (T t : list) {
            String k = key.apply(t);
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(k)) { out.put(names[i], out.get(names[i]) + 1); break; }
            }
        }
        return out;
    }

    /** "SHIPPED" -> "Shipped" */
    static String title(String s) {
        if (s == null || s.isEmpty()) return "";
        String t = s.toLowerCase(java.util.Locale.US).replace('_', ' ');
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Counts of any string field, most common first, at most {@code max} entries. */
    static <T> LinkedHashMap<String, Long> topCounts(List<T> list, Function<T, String> key, int max) {
        Map<String, Long> m = new LinkedHashMap<>();
        if (list != null) for (T t : list) {
            String k = key.apply(t);
            if (k == null || k.isEmpty()) continue;
            m.put(k, m.containsKey(k) ? m.get(k) + 1 : 1L);
        }
        java.util.List<Map.Entry<String, Long>> es = new java.util.ArrayList<>(m.entrySet());
        java.util.Collections.sort(es, (a, b) -> Long.compare(b.getValue(), a.getValue()));
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(max, es.size()); i++) out.put(es.get(i).getKey(), es.get(i).getValue());
        return out;
    }
}

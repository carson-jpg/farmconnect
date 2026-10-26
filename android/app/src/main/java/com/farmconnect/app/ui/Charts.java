package com.farmconnect.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.farmconnect.app.util.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Small native charts (no libraries): columns over time and a donut with a legend. */
final class Charts {
    private Charts() {}

    static final int[] PALETTE = {0xFF2E7D32, 0xFFF9A825, 0xFF1E88E5, 0xFF8E24AA, 0xFFE53935, 0xFF00897B, 0xFF6D4C41};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /** 1200 -> 1.2k, 2500000 -> 2.5M */
    static String compact(double v) {
        if (v >= 1_000_000) return trim(String.format(Locale.US, "%.1f", v / 1_000_000)) + "M";
        if (v >= 1_000) return trim(String.format(Locale.US, v >= 10_000 ? "%.0f" : "%.1f", v / 1_000)) + "k";
        return String.valueOf(Math.round(v));
    }

    private static String trim(String s) { return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s; }

    /** "2026-10" -> "Oct '26"; anything else is shortened. */
    static String monthLabel(String key) {
        if (key != null && key.matches("\\d{4}-\\d{1,2}")) {
            int m = Integer.parseInt(key.substring(5)) - 1;
            if (m >= 0 && m < 12) return MONTHS[m] + " '" + key.substring(2, 4);
        }
        return key == null ? "" : key.length() > 8 ? key.substring(0, 7) + "…" : key;
    }

    /** A column chart of month -> value. */
    static View columns(Context c, Map<String, ? extends Number> data, int color) {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
            labels.add(monthLabel(e.getKey()));
            values.add(e.getValue().doubleValue());
        }
        return new ColumnView(c, labels, values, color);
    }

    /** A donut plus one legend row per slice, added to {@code into}. */
    static void donut(Context c, LinearLayout into, Map<String, ? extends Number> data, String centreLabel) {
        double total = 0;
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<String, ? extends Number> e : data.entrySet()) {
            double v = e.getValue().doubleValue();
            if (v <= 0) continue;
            labels.add(e.getKey());
            values.add(v);
            total += v;
        }
        if (total <= 0) { into.addView(Cards.text(c, I18n.t("No data yet"), 13, 0xFF5E7061, false)); return; }
        into.addView(new DonutView(c, values, centreLabel), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Cards.dp(c, 176)));
        for (int i = 0; i < labels.size(); i++) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Cards.dp(c, 5), 0, 0);
            View dot = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(PALETTE[i % PALETTE.length]);
            dot.setBackground(g);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Cards.dp(c, 11), Cards.dp(c, 11));
            dlp.setMarginEnd(Cards.dp(c, 10));
            row.addView(dot, dlp);
            TextView name = Cards.text(c, labels.get(i), 13, 0xFF1C2B1E, false);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            long n = Math.round(values.get(i));
            row.addView(Cards.text(c, n + "  ·  " + Math.round(values.get(i) * 100 / total) + "%", 13, 0xFF1C2B1E, true));
            into.addView(row);
        }
    }

    private static float sp(Context c, float v) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics()); }

    private static Paint text(Context c, int color, float sp, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(sp(c, sp));
        p.setTextAlign(align);
        if (bold) p.setTypeface(Typeface.DEFAULT_BOLD);
        return p;
    }

    /** Vertical bars with the value above each bar and the month underneath. */
    static final class ColumnView extends View {
        private final List<String> labels;
        private final List<Double> values;
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), axis = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint val, lab, none;

        ColumnView(Context c, List<String> labels, List<Double> values, int color) {
            super(c);
            this.labels = labels;
            this.values = values;
            bar.setColor(color);
            axis.setColor(0xFFE3EBE0);
            axis.setStrokeWidth(Cards.dp(c, 1));
            val = text(c, 0xFF1C2B1E, 11, true, Paint.Align.CENTER);
            lab = text(c, 0xFF5E7061, 10.5f, false, Paint.Align.CENTER);
            none = text(c, 0xFF5E7061, 13, false, Paint.Align.CENTER);
        }

        @Override protected void onMeasure(int w, int h) {
            setMeasuredDimension(resolveSize(Cards.dp(getContext(), 300), w), Cards.dp(getContext(), 200));
        }

        @Override protected void onDraw(Canvas cv) {
            Context c = getContext();
            float w = getWidth(), h = getHeight();
            double max = 0;
            for (double v : values) max = Math.max(max, v);
            if (values.isEmpty() || max <= 0) { cv.drawText(I18n.t("No data yet"), w / 2, h / 2, none); return; }
            float top = Cards.dp(c, 24), bottom = Cards.dp(c, 28), gap = Cards.dp(c, 10);
            int n = values.size();
            float bw = Math.min(Cards.dp(c, 54), (w - gap * (n - 1)) / n);
            float start = (w - (bw * n + gap * (n - 1))) / 2f, base = h - bottom, plot = base - top;
            cv.drawLine(0, base, w, base, axis);
            for (int i = 0; i < n; i++) {
                double v = values.get(i);
                float x = start + i * (bw + gap), ht = v > 0 ? Math.max(Cards.dp(c, 3), (float) (v / max * plot)) : 0;
                RectF r = new RectF(x, base - ht, x + bw, base);
                if (ht > 0) cv.drawRoundRect(r, Cards.dp(c, 5), Cards.dp(c, 5), bar);
                if (v > 0) cv.drawText(compact(v), x + bw / 2, base - ht - Cards.dp(c, 6), val);
                cv.drawText(labels.get(i), x + bw / 2, h - Cards.dp(c, 8), lab);
            }
        }
    }

    /** A ring split into slices, with the total in the middle. */
    static final class DonutView extends View {
        private final List<Double> values;
        private final String centre;
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG), big, small;
        private double total;

        DonutView(Context c, List<Double> values, String centre) {
            super(c);
            this.values = values;
            this.centre = centre == null ? "" : centre;
            for (double v : values) total += v;
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(Cards.dp(c, 26));
            big = text(c, 0xFF1C2B1E, 26, true, Paint.Align.CENTER);
            big.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            small = text(c, 0xFF5E7061, 12, false, Paint.Align.CENTER);
        }

        @Override protected void onMeasure(int w, int h) {
            setMeasuredDimension(resolveSize(Cards.dp(getContext(), 300), w), Cards.dp(getContext(), 176));
        }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), stroke = ring.getStrokeWidth();
            float r = Math.min(w, h) / 2f - stroke / 2f - Cards.dp(getContext(), 4);
            float cx = w / 2f, cy = h / 2f;
            RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
            ring.setColor(0xFFEEF2EE);
            cv.drawArc(oval, 0, 360, false, ring);
            float start = -90f;
            for (int i = 0; i < values.size(); i++) {
                float sweep = (float) (values.get(i) / total * 360.0);
                ring.setColor(PALETTE[i % PALETTE.length]);
                cv.drawArc(oval, start, Math.max(sweep - (values.size() > 1 ? 1.5f : 0f), 0.5f), false, ring);
                start += sweep;
            }
            cv.drawText(String.valueOf(Math.round(total)), cx, cy + sp(getContext(), 4), big);
            cv.drawText(centre, cx, cy + sp(getContext(), 20), small);
        }
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;

/** Tiny helpers for building card-based screens in code. */
final class Cards {
    private Cards() {}

    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    /** A white rounded card; add content to the returned inner vertical layout via {@link #inner(MaterialCardView)}. */
    static MaterialCardView card(Context c, ViewGroup parent, int marginTopDp) {
        MaterialCardView card = new MaterialCardView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, marginTopDp), 0, 0);
        card.setLayoutParams(lp);
        card.setRadius(dp(c, 18));
        card.setCardElevation(dp(c, 2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        LinearLayout in = new LinearLayout(c);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        card.addView(in, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (parent != null) parent.addView(card);
        return card;
    }

    static LinearLayout inner(MaterialCardView card) { return (LinearLayout) card.getChildAt(0); }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView title(Context c, String s) {
        TextView t = text(c, s, 17, 0xFF1C2B1E, true);
        t.setTypeface(Typeface.SERIF, Typeface.BOLD);
        return t;
    }

    static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    /** One stat tile (big number + label). */
    static MaterialCardView stat(Context c, String value, String label) {
        MaterialCardView card = new MaterialCardView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        card.setLayoutParams(lp);
        card.setRadius(dp(c, 16));
        card.setCardElevation(dp(c, 2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        LinearLayout in = new LinearLayout(c);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        TextView v = text(c, value, 20, 0xFF2E7D32, true);
        v.setTypeface(Typeface.SERIF, Typeface.BOLD);
        in.addView(v);
        in.addView(text(c, label, 12, 0xFF5E7061, false));
        card.addView(in);
        return card;
    }

    static LinearLayout statRow(Context c, ViewGroup parent) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (parent != null) parent.addView(row);
        return row;
    }

    /** Horizontal bars: label on the left, filled bar proportional to the value, number on the right. */
    static void bars(Context c, LinearLayout into, java.util.Map<String, ? extends Number> data, int color, String unit) {
        double max = 0;
        for (Number n : data.values()) max = Math.max(max, n.doubleValue());
        if (data.isEmpty() || max <= 0) {
            into.addView(text(c, I18n.t("No data yet"), 13, 0xFF5E7061, false));
            return;
        }
        for (java.util.Map.Entry<String, ? extends Number> e : data.entrySet()) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(c, 6), 0, 0);
            LinearLayout top = new LinearLayout(c);
            top.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(c, e.getKey(), 13, 0xFF1C2B1E, false);
            top.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            double v = e.getValue().doubleValue();
            String shown = (v == Math.floor(v) ? String.valueOf((long) v) : String.format(java.util.Locale.US, "%.1f", v));
            top.addView(text(c, shown + (unit == null ? "" : " " + unit), 13, 0xFF1C2B1E, true));
            row.addView(top);
            LinearLayout track = new LinearLayout(c);
            track.setBackgroundColor(0xFFE3EBE0);
            track.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 8)));
            View fill = new View(c);
            fill.setBackgroundColor(color);
            track.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) (v / max)));
            View rest = new View(c);
            track.addView(rest, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (float) (1 - v / max)));
            LinearLayout.LayoutParams tp = (LinearLayout.LayoutParams) track.getLayoutParams();
            tp.topMargin = dp(c, 3);
            row.addView(track);
            into.addView(row);
        }
    }

    static void center(TextView t) { t.setGravity(Gravity.CENTER); }
}

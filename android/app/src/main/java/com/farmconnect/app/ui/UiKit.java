package com.farmconnect.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;

/** Building blocks for the dashboards: a status banner, a progress stepper and section titles. */
final class UiKit {
    private UiKit() {}

    /** A banner with a coloured left edge: title, message and a status chip. */
    static void hero(Context c, LinearLayout parent, String title, String message, String chip, int color) {
        MaterialCardView card = Cards.card(c, parent, 8);
        LinearLayout in = Cards.inner(card);
        LinearLayout top = new LinearLayout(c);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Cards.title(c, title);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(t);
        TextView pill = new TextView(c);
        pill.setText(chip);
        pill.setTextSize(11.5f);
        pill.setTypeface(Typeface.DEFAULT_BOLD);
        pill.setTextColor(color);
        pill.setPadding(Cards.dp(c, 11), Cards.dp(c, 4), Cards.dp(c, 11), Cards.dp(c, 4));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(Cards.dp(c, 20));
        bg.setColor((color & 0x00FFFFFF) | 0x24000000);
        pill.setBackground(bg);
        top.addView(pill);
        in.addView(top);
        TextView m = Cards.text(c, message, 14, 0xFF5E7061, false);
        m.setPadding(0, Cards.dp(c, 6), 0, 0);
        in.addView(m);
        card.setStrokeColor((color & 0x00FFFFFF) | 0x33000000);
        card.setStrokeWidth(Cards.dp(c, 1));
    }

    /** Dots joined by lines. state: 0 to do, 1 now, 2 done, 3 problem. */
    static void stepper(Context c, LinearLayout parent, String[] labels, int[] state) {
        MaterialCardView card = Cards.card(c, parent, 10);
        LinearLayout in = Cards.inner(card);
        in.addView(Cards.text(c, "Progress", 12, 0xFF6B7A6E, true));
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, Cards.dp(c, 10), 0, 0);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView dot = new TextView(c);
            dot.setGravity(Gravity.CENTER);
            dot.setTextSize(13);
            dot.setTypeface(Typeface.DEFAULT_BOLD);
            int s = state[i];
            int fill = s == 2 ? 0xFF2E7D32 : s == 3 ? 0xFFC62828 : s == 1 ? 0xFFFFFFFF : 0xFFE6ECE5;
            dot.setTextColor(s == 2 || s == 3 ? 0xFFFFFFFF : s == 1 ? 0xFF2E7D32 : 0xFF6B7A6E);
            dot.setText(s == 2 ? "✓" : s == 3 ? "!" : String.valueOf(i + 1));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(fill);
            if (s == 1) bg.setStroke(Cards.dp(c, 2), 0xFF2E7D32);
            dot.setBackground(bg);
            col.addView(dot, new LinearLayout.LayoutParams(Cards.dp(c, 32), Cards.dp(c, 32)));
            TextView l = Cards.text(c, labels[i], 12, s == 0 ? 0xFF6B7A6E : 0xFF1C2B1E, s != 0);
            l.setGravity(Gravity.CENTER);
            l.setPadding(0, Cards.dp(c, 6), 0, 0);
            col.addView(l);
            row.addView(col);
        }
        in.addView(row);
    }

    static TextView section(Context c, LinearLayout parent, String text) {
        TextView t = Cards.text(c, text, 13, 0xFF6B7A6E, true);
        t.setAllCaps(true);
        t.setPadding(Cards.dp(c, 6), Cards.dp(c, 18), 0, Cards.dp(c, 2));
        parent.addView(t);
        return t;
    }

    /** A numbers tile like the web dashboard: a coloured emoji disc, a label, the figure and a small note. */
    static MaterialCardView statCard(Context c, String emoji, String label, String value, String sub, int tint, View.OnClickListener click) {
        MaterialCardView card = new MaterialCardView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Cards.dp(c, 5), Cards.dp(c, 5), Cards.dp(c, 5), Cards.dp(c, 5));
        card.setLayoutParams(lp);
        card.setRadius(Cards.dp(c, 16));
        card.setCardElevation(Cards.dp(c, 2));
        card.setCardBackgroundColor(0xFFFFFFFF);
        if (click != null) { card.setClickable(true); card.setFocusable(true); card.setOnClickListener(click); }
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Cards.dp(c, 12), Cards.dp(c, 14), Cards.dp(c, 12), Cards.dp(c, 14));
        TextView ico = new TextView(c);
        ico.setText(emoji);
        ico.setTextSize(19);
        ico.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor((tint & 0x00FFFFFF) | 0x26000000);
        ico.setBackground(bg);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Cards.dp(c, 42), Cards.dp(c, 42));
        ilp.setMarginEnd(Cards.dp(c, 10));
        row.addView(ico, ilp);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView l = Cards.text(c, label, 12, 0xFF5E7061, false);
        l.setMaxLines(2);
        col.addView(l);
        TextView v = Cards.text(c, value, 20, 0xFF1C2B1E, true);
        v.setTypeface(Typeface.SERIF, Typeface.BOLD);
        v.setMaxLines(1);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(v, 12, 20, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        col.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (sub != null && !sub.isEmpty()) {
            TextView sb = Cards.text(c, sub, 11, 0xFF5E7061, false);
            sb.setMaxLines(2);
            col.addView(sb);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return card;
    }

    /** Lays tiles out in equal columns: 2 on a phone, 3 or 4 on a tablet. A short last row keeps the same tile width. */
    static void grid(Context c, LinearLayout parent, java.util.List<View> tiles) {
        int w = c.getResources().getConfiguration().screenWidthDp;
        int cols = w >= 900 ? 4 : w >= 600 ? 3 : 2;
        for (int i = 0; i < tiles.size(); i += cols) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            for (int k = 0; k < cols; k++) {
                if (i + k < tiles.size()) row.addView(tiles.get(i + k));
                else {
                    View pad = new View(c);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
                    lp.setMargins(Cards.dp(c, 5), 0, Cards.dp(c, 5), 0);
                    row.addView(pad, lp);
                }
            }
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(-Cards.dp(c, 5), 0, -Cards.dp(c, 5), 0);
            parent.addView(row, rlp);
        }
    }

    /** A white card with a title; returns the layout to put the chart in. */
    static LinearLayout chartCard(Context c, LinearLayout parent, String title) {
        MaterialCardView card = Cards.card(c, parent, 10);
        LinearLayout in = Cards.inner(card);
        in.addView(Cards.title(c, title));
        View gap = Cards.spacer(c, 6);
        in.addView(gap);
        return in;
    }
}

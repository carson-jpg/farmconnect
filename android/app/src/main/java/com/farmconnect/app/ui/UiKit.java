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
}

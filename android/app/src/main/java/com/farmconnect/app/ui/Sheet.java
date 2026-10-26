package com.farmconnect.app.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

/**
 * A bottom sheet for the details of one item (order, quotation, request...): a title, a status badge,
 * label/value rows, and a stack of action buttons. Replaces plain text dialogs.
 */
final class Sheet {
    private static final int GREEN = 0xFF2E7D32, RED = 0xFFC62828, INK = 0xFF1C2B1E, MUTED = 0xFF6B7A6E, LINE = 0xFFE3EBE2;

    private final Activity a;
    private final BottomSheetDialog dialog;
    private final LinearLayout root;
    private final LinearLayout actions;

    private Sheet(Activity a, String title) {
        this.a = a;
        dialog = new BottomSheetDialog(a);
        NestedScrollView scroll = new NestedScrollView(a);
        root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(24));
        scroll.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(20);
        t.setTextColor(0xFF1B3A1F);
        t.setTypeface(Typeface.SERIF, Typeface.BOLD);
        root.addView(t);
        actions = new LinearLayout(a);
        actions.setOrientation(LinearLayout.VERTICAL);
        dialog.setContentView(scroll);
    }

    static Sheet of(Activity a, String title) { return new Sheet(a, title); }

    private int dp(int v) { return Cards.dp(a, v); }

    /** A rounded status chip under the title. */
    Sheet badge(String text, int color) {
        if (text == null || text.isEmpty()) return this;
        TextView b = new TextView(a);
        b.setText(text);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(color);
        b.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor((color & 0x00FFFFFF) | 0x24000000);
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        root.addView(b, lp);
        return this;
    }

    /** A label above a value. Empty values are skipped. */
    Sheet field(String label, String value) {
        if (value == null || value.trim().isEmpty()) return this;
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, 0);
        TextView l = new TextView(a);
        l.setText(label);
        l.setTextSize(11.5f);
        l.setTextColor(MUTED);
        l.setAllCaps(true);
        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(15);
        v.setTextColor(INK);
        row.addView(l);
        row.addView(v);
        root.addView(row);
        return this;
    }

    /** A highlighted line, e.g. the total. */
    Sheet strong(String label, String value) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, 0);
        TextView l = new TextView(a);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(MUTED);
        l.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(18);
        v.setTypeface(Typeface.SERIF, Typeface.BOLD);
        v.setTextColor(GREEN);
        row.addView(l);
        row.addView(v);
        root.addView(row);
        return this;
    }

    Sheet note(String text) {
        if (text == null || text.trim().isEmpty()) return this;
        TextView n = new TextView(a);
        n.setText(text);
        n.setTextSize(14);
        n.setTextColor(INK);
        n.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0xFFF3F7F0);
        n.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        root.addView(n, lp);
        return this;
    }

    /** A row of photos (loaded from signed links). */
    Sheet photos(java.util.List<String> urls) {
        if (urls == null || urls.isEmpty()) return this;
        android.widget.HorizontalScrollView sv = new android.widget.HorizontalScrollView(a);
        sv.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = new LinearLayout(a);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        for (String u : urls) {
            android.widget.ImageView iv = new android.widget.ImageView(a);
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(130), dp(130));
            ip.setMarginEnd(dp(8));
            strip.addView(iv, ip);
            com.bumptech.glide.Glide.with(a).load(com.farmconnect.app.data.ApiClient.absolute(u)).centerCrop().into(iv);
        }
        sv.addView(strip);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        root.addView(sv, lp);
        return this;
    }

    Sheet divider() {
        View d = new View(a);
        d.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(16);
        root.addView(d, lp);
        return this;
    }

    private Sheet button(String label, int kind, Runnable r) {
        if (actions.getParent() == null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(18);
            root.addView(actions, lp);
        }
        MaterialButton b = kind == 1
                ? new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
                : new MaterialButton(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setCornerRadius(dp(14));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setMinHeight(dp(50));
        if (kind == 0) { b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN)); b.setTextColor(0xFFFFFFFF); }
        else if (kind == 2) { b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RED)); b.setTextColor(0xFFFFFFFF); }
        else { b.setTextColor(GREEN); b.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFB9D6BC)); }
        b.setOnClickListener(v -> { dialog.dismiss(); if (r != null) r.run(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        actions.addView(b, lp);
        return this;
    }

    Sheet primary(String label, Runnable r) { return button(label, 0, r); }
    Sheet secondary(String label, Runnable r) { return button(label, 1, r); }
    Sheet danger(String label, Runnable r) { return button(label, 2, r); }

    void show() { dialog.show(); }
}

package com.farmconnect.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.ui.AdminHomeActivity;
import com.farmconnect.app.ui.BuyerHomeActivity;
import com.farmconnect.app.ui.FarmerHomeActivity;
import com.farmconnect.app.ui.LoginActivity;
import com.farmconnect.app.ui.ReviewerHomeActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public class Ui {
    public static void toast(Context c, String m) { Toast.makeText(c, I18n.t(m), Toast.LENGTH_SHORT).show(); }

    public static String kes(double v) { return String.format(Locale.US, I18n.t("KES %,.0f"), v); }

    /** "2026-10-02T06:12:33.123Z" -> "02 Oct 2026, 09:12" in the phone's time zone. */
    public static String dateTime(String iso) {
        if (iso == null || iso.length() < 19) return iso == null ? "" : iso;
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = in.parse(iso.substring(0, 19));
            return new SimpleDateFormat("dd MMM yyyy, HH:mm", I18n.locale()).format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    /** Colored order-status pill. */
    public static void styleStatus(TextView tv, String status) {
        String s = status == null ? "" : status;
        tv.setText(I18n.t(s.isEmpty() ? "" : s.charAt(0) + s.substring(1).toLowerCase(Locale.US)));
        int c;
        switch (s) {
            case "PENDING": c = 0xFFF9A825; break;
            case "CONFIRMED": c = 0xFF1E88E5; break;
            case "SHIPPED": c = 0xFF8E24AA; break;
            case "DELIVERED": c = 0xFF2E7D32; break;
            case "CANCELLED": c = 0xFFC62828; break;
            default: c = 0xFF757575;
        }
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setBackgroundTintList(ColorStateList.valueOf(c));
    }

    /** "2026-11-05T12:00:00Z" -> "05 Nov 2026" (date only, no time-zone shifting). */
    public static String dateOnly(String iso) {
        if (iso == null || iso.length() < 10) return iso == null ? "" : iso;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.substring(0, 10));
            return new SimpleDateFormat("dd MMM yyyy", I18n.locale()).format(d);
        } catch (Exception e) {
            return iso.substring(0, 10);
        }
    }

    public static String paymentLabel(String m) {
        if ("MPESA_ON_DELIVERY".equals(m)) return I18n.t("M-Pesa on delivery");
        return I18n.t("Cash on delivery");
    }

    /** First letter of a name, for avatar circles. */
    public static String initial(String name) {
        return name == null || name.trim().isEmpty() ? "?" : name.trim().substring(0, 1).toUpperCase(Locale.US);
    }

    public static String verificationLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "PENDING": return "Pending verification";
            case "UNDER_REVIEW": return "Under review";
            case "VERIFIED": return "✓ Verified";
            case "REJECTED": return "Rejected";
            default: return I18n.t("Not submitted");
        }
    }

    public static int verificationColor(String status) {
        if (status == null) return 0xFF757575;
        switch (status) {
            case "PENDING": return 0xFFF9A825;
            case "UNDER_REVIEW": return 0xFF8E24AA;
            case "VERIFIED": return 0xFF2E7D32;
            case "REJECTED": return 0xFFC62828;
            default: return 0xFF757575;
        }
    }

    /** Colored verification-status pill. */
    public static void styleVerification(TextView tv, String status) {
        tv.setText(I18n.t(verificationLabel(status)));
        tv.setBackgroundResource(R.drawable.bg_chip);
        tv.setBackgroundTintList(ColorStateList.valueOf(verificationColor(status)));
    }

    public static void home(Activity a) {
        String role = Session.role();
        Class<?> target;
        if ("FARMER".equals(role)) target = FarmerHomeActivity.class;
        else if ("ADMIN".equals(role)) target = AdminHomeActivity.class;
        else if ("OFFICER".equals(role)) target = ReviewerHomeActivity.class;
        else target = BuyerHomeActivity.class;
        Intent i = new Intent(a, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
    }

    public static void logout(Activity a) {
        Session.clear();
        Intent i = new Intent(a, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
    }

    /** Observe a result: shows progress, toasts errors, calls ok on success. */
    public static <T> void watch(AppCompatActivity a, LiveData<Resource<T>> ld, View progress, Consumer<T> ok) {
        ld.observe(a, r -> {
            if (progress != null) progress.setVisibility(r.status == Resource.Status.LOADING ? View.VISIBLE : View.GONE);
            if (r.status == Resource.Status.SUCCESS) ok.accept(r.data);
            else if (r.status == Resource.Status.ERROR) toast(a, r.message);
        });
    }
}
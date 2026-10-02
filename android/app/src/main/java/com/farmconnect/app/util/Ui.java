package com.farmconnect.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.ui.BuyerHomeActivity;
import com.farmconnect.app.ui.FarmerHomeActivity;
import com.farmconnect.app.ui.LoginActivity;
import java.util.Locale;
import java.util.function.Consumer;

public class Ui {
    public static void toast(Context c, String m) { Toast.makeText(c, m, Toast.LENGTH_SHORT).show(); }

    public static String kes(double v) { return String.format(Locale.US, "KES %,.0f", v); }

    public static void home(Activity a) {
        Class<?> target = "FARMER".equals(Session.role()) ? FarmerHomeActivity.class : BuyerHomeActivity.class;
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

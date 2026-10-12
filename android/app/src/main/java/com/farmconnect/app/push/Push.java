package com.farmconnect.app.push;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.farmconnect.app.R;
import com.farmconnect.app.data.ApiClient;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.ui.NotificationsActivity;
import com.farmconnect.app.util.I18n;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Push notifications through Firebase Cloud Messaging.
 * Without app/google-services.json the app still builds and runs; push is simply off.
 */
public final class Push {
    public static final String CHANNEL_ID = "farmconnect_alerts";
    private static final String PREFS = "farmconnect_push";

    private Push() {}

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private static boolean firebaseReady(Context c) {
        try { return !FirebaseApp.getApps(c).isEmpty(); } catch (Exception e) { return false; }
    }

    /** Android 8+ needs a channel before any notification can be shown. Call once at start-up. */
    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "FarmConnect alerts", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Orders, messages, market prices and county notices");
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    /** Call from a dashboard: asks for the notification permission once (Android 13+), then registers this phone. */
    public static void sync(Activity a) {
        if (!Session.isLoggedIn()) return;
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !prefs(a).getBoolean("asked", false)) {
            prefs(a).edit().putBoolean("asked", true).apply();
            ActivityCompat.requestPermissions(a, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4711);
        }
        if (!firebaseReady(a)) return;
        final Context app = a.getApplicationContext();
        try {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> register(app, token));
        } catch (Exception ignored) { /* Firebase not configured */ }
    }

    /** Tell the server about this phone. Skipped when the same phone is already registered for this user. */
    static void register(Context c, String token) {
        if (token == null || token.isEmpty() || !Session.isLoggedIn()) return;
        final SharedPreferences p = prefs(c);
        final long userId = Session.id();
        if (token.equals(p.getString("token", null)) && p.getLong("user", -1) == userId) return;
        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("platform", "android");
        ApiClient.get().registerDevice(body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) {
                if (r.isSuccessful()) p.edit().putString("token", token).putLong("user", userId).apply();
            }
            @Override public void onFailure(Call<Void> call, Throwable t) { /* tried again at the next dashboard visit */ }
        });
    }

    /** Called on logout, before the session is cleared: stop sending this user's alerts to this phone. */
    public static void forget(Context c) {
        SharedPreferences p = prefs(c);
        String token = p.getString("token", null);
        String bearer = Session.token();
        p.edit().remove("token").remove("user").apply();
        if (token != null && bearer != null) {
            ApiClient.get().unregisterDevice("Bearer " + bearer, token).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> r) {}
                @Override public void onFailure(Call<Void> call, Throwable t) {}
            });
        }
        if (firebaseReady(c)) {
            try { FirebaseMessaging.getInstance().deleteToken(); } catch (Exception ignored) { /* best effort */ }
        }
    }

    /** Builds the system notification from the data the server sent. Texts follow the app language. */
    @SuppressLint("MissingPermission")
    static void show(Context c, Map<String, String> data) {
        if (!Session.isLoggedIn() || data == null) return;
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        String title = I18n.t(nz(data.get("title")));
        String body = I18n.t(nz(data.get("body")));
        if (title.isEmpty() && body.isEmpty()) return;
        int id;
        try { id = Integer.parseInt(nz(data.get("notificationId"))); } catch (NumberFormatException e) { id = (int) (System.currentTimeMillis() & 0x7fffffff); }

        Intent open = new Intent(c, NotificationsActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(0xFF2E7D32)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManagerCompat.from(c).notify(id, b.build());
    }

    private static String nz(String s) { return s == null ? "" : s; }
}

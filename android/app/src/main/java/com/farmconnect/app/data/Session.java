package com.farmconnect.app.data;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {
    private static SharedPreferences p;

    public static void init(Context c) { p = c.getSharedPreferences("farmconnect", Context.MODE_PRIVATE); }
    public static void save(Models.AuthResponse a) {
        p.edit().putString("token", a.token).putString("role", a.role).putString("name", a.name).putLong("id", a.id).apply();
    }
    public static long id() { return p.getLong("id", -1); }
    public static String token() { return p.getString("token", null); }
    public static String role() { return p.getString("role", null); }
    public static String name() { return p.getString("name", null); }
    public static boolean isLoggedIn() { return token() != null; }
    public static void clear() { p.edit().clear().apply(); }
}

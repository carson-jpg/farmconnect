package com.farmconnect.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.farmconnect.app.R;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * English / Kiswahili.
 *  - Text in layouts comes from string resources (values/ and values-sw/), so Android swaps it automatically.
 *  - Text built in code goes through {@link #t(String)}, which looks the English text up in res/raw/sw_dict.txt.
 * Text that has no Swahili entry simply stays in English, so nothing ever breaks.
 * The chosen language is remembered in its own preferences file so it survives logout.
 */
public final class I18n {
    private static final String FILE = "farmconnect_lang";
    private static String lang = "en";
    private static Map<String, String> sw = new HashMap<>();

    private I18n() {}

    public static void init(Context app) {
        SharedPreferences p = app.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String saved = p.getString("lang", null);
        if (saved == null) saved = "sw".equals(Locale.getDefault().getLanguage()) ? "sw" : "en";
        lang = saved;
        loadDictionary(app);
        applyLocale();
    }

    private static void loadDictionary(Context c) {
        Map<String, String> m = new HashMap<>();
        try (InputStream in = c.getResources().openRawResource(R.raw.sw_dict);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                int tab = line.indexOf('\t');
                if (tab <= 0) continue;
                // keys and values are stored without surrounding whitespace, so the dictionary still works even if a
                // tool strips trailing spaces from the file; t() puts the original spaces back around the result.
                m.put(unescape(line.substring(0, tab)).trim(), unescape(line.substring(tab + 1)).trim());
            }
        } catch (Exception ignored) { }
        sw = m;
    }

    private static String unescape(String s) { return s.replace("\\n", "\n").replace("\\t", "\t"); }

    private static void applyLocale() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    public static boolean isSw() { return "sw".equals(lang); }
    public static String lang() { return lang; }
    public static Locale locale() { return new Locale(lang); }

    /** Translate a piece of English text built in code. Leading / trailing spaces are kept. */
    public static String t(String s) {
        if (s == null || s.isEmpty() || !isSw()) return s;
        int a = 0, b = s.length();
        while (a < b && Character.isWhitespace(s.charAt(a))) a++;
        while (b > a && Character.isWhitespace(s.charAt(b - 1))) b--;
        if (a == b) return s;
        String hit = sw.get(s.substring(a, b));
        return hit == null ? s : s.substring(0, a) + hit + s.substring(b);
    }

    /** Switch language and reopen the app on the right home screen so every screen is rebuilt. */
    public static void set(Activity a, String newLang) {
        if (newLang.equals(lang)) return;
        a.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("lang", newLang).apply();
        lang = newLang;
        applyLocale();
        if (com.farmconnect.app.data.Session.isLoggedIn()) Ui.home(a);
        else a.recreate();
    }

    public static void chooseLanguage(Activity a) {
        final String[] codes = {"en", "sw"};
        final String[] names = {"English", "Kiswahili"};
        int current = isSw() ? 1 : 0;
        new AlertDialog.Builder(a).setTitle(t("Language") + " / Lugha")
                .setSingleChoiceItems(names, current, (d, which) -> { d.dismiss(); set(a, codes[which]); })
                .setNegativeButton(t("Cancel"), null).show();
    }
}

package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Weather;
import com.farmconnect.app.data.Models.WeatherDay;
import com.farmconnect.app.databinding.ActivitySimplePageBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Live 7-day forecast for Kitale with plain-language farming advice. */
public class WeatherActivity extends AppCompatActivity {
    private ActivitySimplePageBinding b;
    private CommunityViewModel vm;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySimplePageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        b.tvTitle.setText(I18n.t("Weather & advice"));
        b.tvSubtitle.setText(I18n.t("Loading the forecast..."));
        b.btnBack.setOnClickListener(v -> finish());
        Ui.watch(this, vm.weather(), b.progress, this::render);
    }

    private static String icon(WeatherDay d) {
        if (d.rainMm >= 10) return "⛈";
        if (d.rainMm >= 2 || d.rainProb >= 60) return "🌧";
        if (d.rainProb >= 30) return "⛅";
        return "☀️";
    }

    private static String dayName(String iso) {
        try {
            return new SimpleDateFormat("EEE d MMM", com.farmconnect.app.util.I18n.locale()).format(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso));
        } catch (Exception e) { return iso; }
    }

    private void render(Weather w) {
        android.content.Context c = this;
        LinearLayout content = b.content;
        content.removeAllViews();
        b.tvSubtitle.setText("📍 " + w.place);

        MaterialCardView now = Cards.card(c, content, 0);
        LinearLayout in = Cards.inner(now);
        TextView temp = Cards.text(c, Math.round(w.tempC) + "°C", 44, 0xFF2E7D32, true);
        temp.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        in.addView(temp);
        in.addView(Cards.text(c, I18n.t("Right now in ") + w.place, 13, 0xFF5E7061, false));
        in.addView(Cards.spacer(c, 8));
        in.addView(Cards.text(c, I18n.t("💧 Humidity ") + w.humidity + I18n.t("%     💨 Wind ") + Math.round(w.windKmh) + I18n.t(" km/h     🌧 Rain now ")
                + String.format(Locale.US, "%.1f", w.rainNowMm) + I18n.t(" mm"), 13, 0xFF1C2B1E, false));

        MaterialCardView adv = Cards.card(c, content, 12);
        LinearLayout ai = Cards.inner(adv);
        ai.addView(Cards.title(c, I18n.t("🌱 What this means for your farm")));
        for (String a : w.advisories) {
            TextView t = Cards.text(c, "•  " + a, 14, 0xFF1C2B1E, false);
            t.setPadding(0, Cards.dp(c, 8), 0, 0);
            ai.addView(t);
        }

        MaterialCardView week = Cards.card(c, content, 12);
        LinearLayout wi = Cards.inner(week);
        wi.addView(Cards.title(c, I18n.t("Next 7 days")));
        for (WeatherDay d : w.days) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Cards.dp(c, 9), 0, 0);
            row.addView(Cards.text(c, icon(d), 22, 0xFF000000, false));
            TextView day = Cards.text(c, "  " + dayName(d.date), 14, 0xFF1C2B1E, true);
            row.addView(day, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Cards.text(c, Math.round(d.minC) + "° – " + Math.round(d.maxC) + "°     ", 13, 0xFF1C2B1E, false));
            row.addView(Cards.text(c, String.format(Locale.US, I18n.t("%.1f mm · %d%%"), d.rainMm, d.rainProb), 13, 0xFF2E7D32, true));
            wi.addView(row);
        }
        content.addView(Cards.spacer(c, 12));
        TextView src = Cards.text(c, I18n.t("Forecast data: Open-Meteo. Advice is general guidance. Ask your county extension officer for crop-specific recommendations."), 11, 0xFF5E7061, false);
        content.addView(src);
    }
}

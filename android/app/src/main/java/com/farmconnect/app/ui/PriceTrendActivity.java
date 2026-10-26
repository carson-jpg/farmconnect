package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.TrendResponse;
import com.farmconnect.app.databinding.ActivitySimplePageBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;
import com.google.android.material.card.MaterialCardView;
import java.util.Locale;

/** Is the price of a crop rising or falling? A chart of recent prices plus a simple estimate for the next two weeks. */
public class PriceTrendActivity extends AppCompatActivity {
    private ActivitySimplePageBinding b;
    private CommunityViewModel vm;
    private String crop, market;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySimplePageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        crop = getIntent().getStringExtra("crop");
        market = getIntent().getStringExtra("market");
        b.tvTitle.setText(Labels.cropIcon(crop) + "  " + crop);
        b.tvSubtitle.setText(market == null || market.isEmpty() ? I18n.t("All markets") : market);
        b.btnBack.setOnClickListener(v -> finish());
        Ui.watch(this, vm.priceTrend(crop, market, 90), b.progress, this::render);
    }

    private void render(TrendResponse t) {
        android.content.Context c = this;
        LinearLayout content = b.content;
        content.removeAllViews();

        MaterialCardView head = Cards.card(c, content, 0);
        LinearLayout in = Cards.inner(head);
        String arrow = "RISING".equals(t.direction) ? "▲ " + I18n.t("Rising") : "FALLING".equals(t.direction) ? "▼ " + I18n.t("Falling")
                : "STABLE".equals(t.direction) ? "▬ " + I18n.t("Steady") : I18n.t("Not enough data yet");
        int color = "RISING".equals(t.direction) ? 0xFF2E7D32 : "FALLING".equals(t.direction) ? 0xFFC62828 : 0xFF5E7061;
        TextView big = Cards.text(c, arrow, 26, color, true);
        in.addView(big);
        if (!"NOT_ENOUGH_DATA".equals(t.direction))
            in.addView(Cards.text(c, String.format(Locale.US, "%+.1f%% ", t.changePct) + I18n.t("over this period"), 13, 0xFF5E7061, false));
        in.addView(Cards.spacer(c, 8));
        in.addView(Cards.text(c, t.summary == null ? "" : t.summary, 14, 0xFF1C2B1E, false));

        MaterialCardView chart = Cards.card(c, content, 12);
        LinearLayout ci = Cards.inner(chart);
        ci.addView(Cards.title(c, I18n.t("Price per ") + (t.unit == null ? "" : t.unit) + " (KES)"));
        TrendChartView v = new TrendChartView(c);
        v.set(t.history, t.forecast);
        ci.addView(v);
        ci.addView(Cards.text(c, I18n.t("Green: recorded prices.   Amber dashed: estimate for the next 1 to 2 weeks."), 12, 0xFF5E7061, false));

        MaterialCardView nums = Cards.card(c, content, 12);
        LinearLayout ni = Cards.inner(nums);
        ni.addView(Cards.title(c, I18n.t("In this period")));
        ni.addView(Cards.text(c, I18n.t("Lowest: ") + Ui.kes(t.low) + "\n" + I18n.t("Highest: ") + Ui.kes(t.high) + "\n" + I18n.t("Average: ") + Ui.kes(t.average), 14, 0xFF1C2B1E, false));
        if (t.forecast != null && !t.forecast.isEmpty()) {
            StringBuilder f = new StringBuilder();
            for (com.farmconnect.app.data.Models.TrendPoint p : t.forecast) f.append(Ui.dateOnly(p.date)).append(":  ").append(Ui.kes(p.price)).append('\n');
            ni.addView(Cards.spacer(c, 8));
            ni.addView(Cards.text(c, I18n.t("Estimates (") + I18n.t(t.confidence == null ? "LOW" : t.confidence) + I18n.t(" confidence)") + "\n" + f.toString().trim(), 14, 0xFFB26A00, false));
        }
        content.addView(Cards.text(c, I18n.t("This is a guide worked out from recorded prices. It cannot know about weather, supply or holidays."), 12, 0xFF5E7061, false));
    }
}

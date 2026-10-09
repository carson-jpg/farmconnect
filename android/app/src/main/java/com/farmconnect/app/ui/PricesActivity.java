package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.Price;
import com.farmconnect.app.data.Models.PricePoint;
import com.farmconnect.app.data.Models.PriceRequest;
import com.farmconnect.app.util.Ui;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Daily market price board: compare a crop across markets, see the trend, subscribe to price alerts. */
public class PricesActivity extends BaseListActivity {
    private List<Price> all = new ArrayList<>();

    @Override protected String screenTitle() { return I18n.t("Market prices"); }
    @Override protected boolean searchable() { return true; }
    @Override protected String searchHint() { return I18n.t("Search a crop or market..."); }
    @Override protected String fabText() { return Labels.isStaff() ? I18n.t("Post price") : null; }
    @Override protected String emptyText() { return I18n.t("No prices posted yet.\nCounty officers post the daily market prices here."); }

    private static long daysOld(String iso) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.substring(0, 10));
            return (System.currentTimeMillis() - d.getTime()) / 86_400_000L;
        } catch (Exception e) { return 0; }
    }

    @Override protected void load() {
        Ui.watch(this, vm.prices(query()), b.progress, list -> {
            all = list;
            List<Row> rows = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Price p = list.get(i);
                boolean firstOfCrop = i == 0 || !list.get(i - 1).crop.equalsIgnoreCase(p.crop);
                boolean lastOfCrop = i == list.size() - 1 || !list.get(i + 1).crop.equalsIgnoreCase(p.crop);
                boolean multiple = !(firstOfCrop && lastOfCrop);
                StringBuilder meta = new StringBuilder(Ui.dateOnly(p.date));
                long age = daysOld(p.date);
                if (age > 14) meta.append("  ⚠ ").append(age).append(I18n.t(" days old"));
                if (p.previousPrice != null) meta.append(I18n.t("  ·  was ")).append(Ui.kes(p.previousPrice));
                if (multiple && firstOfCrop) meta.append(I18n.t("  ·  💚 Lowest in county"));
                if (multiple && lastOfCrop) meta.append(I18n.t("  ·  🔶 Highest in county"));
                Row r = Row.of(Labels.cropIcon(p.crop), p.crop + " · " + p.market)
                        .sub(Ui.kes(p.price) + I18n.t(" per ") + p.unit)
                        .meta(meta.toString())
                        .click(() -> details(p));
                if (p.changePct != null && p.changePct != 0) {
                    boolean up = p.changePct > 0;
                    r.badge((up ? "▲ " : "▼ ") + String.format(Locale.US, "%.1f%%", Math.abs(p.changePct)), up ? 0xFF2E7D32 : 0xFFC62828);
                } else if (p.subscribed) r.badge(I18n.t("🔔 Alert on"));
                rows.add(r);
            }
            show(rows, list.size() + (list.size() == 1 ? I18n.t(" price") : I18n.t(" prices")));
        });
    }

    private void details(Price p) {
        Ui.watch(this, vm.priceHistory(p.crop, p.market), b.progress, hist -> {
            StringBuilder m = new StringBuilder(I18n.t("Across markets (latest):\n"));
            for (Price o : all) if (o.crop.equalsIgnoreCase(p.crop))
                m.append("• ").append(o.market).append(": ").append(Ui.kes(o.price)).append(" / ").append(o.unit).append('\n');
            m.append(I18n.t("\nRecent at ")).append(p.market).append(":\n");
            int n = 0;
            for (PricePoint h : hist) {
                if (n++ >= 10) break;
                m.append(Ui.dateOnly(h.date)).append("   ").append(Ui.kes(h.price)).append(" / ").append(h.unit).append('\n');
            }
            AlertDialog.Builder d = new AlertDialog.Builder(this)
                    .setTitle(Labels.cropIcon(p.crop) + "  " + p.crop + " · " + p.market)
                    .setMessage(m.toString().trim())
                    .setNegativeButton(I18n.t("Close"), null)
                    .setPositiveButton(p.subscribed ? I18n.t("🔕 Stop alerts") : I18n.t("🔔 Alert me on changes"), (x, y) ->
                            Ui.watch(this, vm.togglePriceAlert(p.crop), b.progress, t -> {
                                Ui.toast(this, t.subscribed ? I18n.t("You will be notified when ") + p.crop + I18n.t(" prices change") : I18n.t("Alerts turned off"));
                                load();
                            }));
            if (Labels.isStaff()) d.setNeutralButton(I18n.t("Delete latest"), (x, y) ->
                    Ui.watch(this, vm.deletePrice(p.id), b.progress, r -> { Ui.toast(this, I18n.t("Price removed")); load(); }));
            d.show();
        });
    }

    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("crop", I18n.t("Crop or product (e.g. Maize, Beans, Milk)"), true));
        f.add(Forms.text("market", I18n.t("Market (e.g. Kitale, Endebess)"), true));
        f.add(Forms.text("unit", I18n.t("Per unit (e.g. 90kg bag, kg, litre)"), true).value(I18n.t("90kg bag")));
        f.add(Forms.number("price", I18n.t("Price in KES"), true));
        f.add(Forms.date("date", I18n.t("Date (leave empty for today)"), false));
        Forms.show(this, I18n.t("Post a market price"), I18n.t("Post"), f, v -> {
            PriceRequest r = new PriceRequest();
            r.crop = v.get("crop");
            r.market = v.get("market");
            r.unit = v.get("unit");
            r.price = Double.valueOf(v.get("price"));
            r.date = v.get("date").isEmpty() ? null : v.get("date");
            Ui.watch(this, vm.postPrice(r), b.progress, p -> {
                Ui.toast(this, I18n.t("Price posted. Subscribers were notified."));
                load();
            });
        });
    }
}

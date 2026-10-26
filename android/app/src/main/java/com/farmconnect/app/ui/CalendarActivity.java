package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import com.farmconnect.app.data.Models.CalendarItem;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * The farmer's job calendar: what to weed, fertilise, spray or harvest and when, worked out from the planting records
 * they keep in the app, with advice from the weather forecast.
 */
public class CalendarActivity extends BaseListActivity {

    @Override protected String screenTitle() { return I18n.t("Farm calendar"); }
    @Override protected String emptyText() {
        return I18n.t("No jobs due yet.\nRecord what you plant in Records and your jobs will appear here.");
    }

    @Override protected void load() {
        Ui.watch(this, vm.calendar(), b.progress, c -> {
            List<Row> rows = new ArrayList<>();
            if (c.plantNow != null && !c.plantNow.isEmpty()) {
                String note = c.plantNowNote == null ? "" : "\n" + I18n.t(c.plantNowNote);
                rows.add(Row.of("🌱", I18n.t("Usually planted around now"))
                        .sub(android.text.TextUtils.join(", ", c.plantNow) + note)
                        .meta(c.region == null ? null : I18n.t(c.region)));
            }
            for (CalendarItem i : c.items) {
                boolean overdue = "OVERDUE".equals(i.status), due = "DUE_NOW".equals(i.status);
                StringBuilder sub = new StringBuilder(i.advice == null ? "" : i.advice);
                if (i.weatherNote != null && !i.weatherNote.isEmpty())
                    sub.append(sub.length() > 0 ? "\n" : "").append(i.goodToGo ? "🌤 " : "⚠ ").append(i.weatherNote);
                rows.add(Row.of(i.icon == null ? "🌱" : i.icon, i.crop + " · " + i.title)
                        .sub(sub.toString())
                        .meta(I18n.t("Planted ") + i.planted + "  ·  " + I18n.t("do it ") + i.from + " – " + i.to)
                        .badge(overdue ? I18n.t("Overdue") : due ? I18n.t("Do now") : I18n.t("Soon"),
                                overdue ? 0xFFC62828 : due ? 0xFF2E7D32 : 0xFF8D6E00));
            }
            String sub = (c.place == null ? "" : c.place) + (c.weatherKnown ? "" : "  ·  " + I18n.t("no forecast right now"));
            if (rows.isEmpty() || (rows.size() == 1 && c.items.isEmpty())) { show(rows, sub); return; }
            show(rows, sub + (c.disclaimer == null ? "" : "\n" + I18n.t(c.disclaimer)));
        });
    }
}

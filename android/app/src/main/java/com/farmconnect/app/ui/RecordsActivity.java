package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.Farm;
import com.farmconnect.app.data.Models.FarmRecord;
import com.farmconnect.app.data.Models.RecordRequest;
import com.farmconnect.app.data.Models.RecordSummary;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The farmer's digital farm book: planting, harvest, livestock, expenses, income and notes, with totals on top. */
public class RecordsActivity extends BaseListActivity {
    private List<Farm> farms = new ArrayList<>();

    @Override protected String screenTitle() { return I18n.t("Farm records"); }
    @Override protected String fabText() { return I18n.t("Add record"); }
    @Override protected String emptyText() { return I18n.t("No records yet.\nTap “Add record” to log planting, harvest, livestock, expenses or income."); }

    @Override protected String[][] chips() {
        return new String[][]{{"", "All"}, {"PLANTING", "Planting"}, {"HARVEST", "Harvest"}, {"LIVESTOCK", "Livestock"},
                {"EXPENSE", "Expenses"}, {"INCOME", "Income"}, {"NOTE", "Notes"}};
    }

    @Override protected void load() {
        Ui.watch(this, vm.myFarms(), null, f -> farms = f);
        Ui.watch(this, vm.recordSummary(), null, this::summary);
        Ui.watch(this, vm.records(chip), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (FarmRecord r : list) {
                String detail;
                if ("EXPENSE".equals(r.type) || "INCOME".equals(r.type)) detail = r.amount == null ? "" : Ui.kes(r.amount);
                else {
                    String q = r.quantity == null ? "" : trim(r.quantity);
                    String u = r.unit == null || r.unit.isEmpty()
                            ? ("PLANTING".equals(r.type) ? "acres" : "LIVESTOCK".equals(r.type) ? "animals" : "HARVEST".equals(r.type) ? "kg" : "") : r.unit;
                    detail = (q + " " + u).trim();
                }
                if (r.notes != null && !r.notes.isEmpty()) detail += (detail.isEmpty() ? "" : "  ·  ") + r.notes;
                rows.add(Row.of(Labels.recordIcon(r.type), r.title).sub(detail)
                        .meta(Ui.dateOnly(r.date) + " · " + r.farmName)
                        .badge(Labels.name(Labels.RECORD_TYPES, Labels.RECORD_NAMES, r.type),
                                "EXPENSE".equals(r.type) ? 0xFFC62828 : 0xFF2E7D32)
                        .click(() -> form(r))
                        .action(I18n.t("Delete"), () -> new AlertDialog.Builder(this)
                                .setTitle(I18n.t("Delete this record?"))
                                .setPositiveButton(I18n.t("Delete"), (d, w) -> Ui.watch(this, vm.deleteRecord(r.id), b.progress, x -> load()))
                                .setNegativeButton(I18n.t("Cancel"), null).show()));
            }
            show(rows, list.size() + (list.size() == 1 ? I18n.t(" record") : I18n.t(" records")));
        });
    }

    private static String trim(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.format(java.util.Locale.US, "%.1f", v); }

    private void summary(RecordSummary s) {
        LinearLayout top = b.topArea;
        top.removeAllViews();
        top.setPadding(dp(10), dp(8), dp(10), 0);
        LinearLayout row1 = Cards.statRow(this, top);
        row1.addView(Cards.stat(this, Ui.kes(s.income + s.platformSales), I18n.t("Money in")));
        row1.addView(Cards.stat(this, Ui.kes(s.expenses), I18n.t("Money out")));
        row1.addView(Cards.stat(this, Ui.kes(s.profit), I18n.t("Profit")));
        StringBuilder sb = new StringBuilder();
        if (s.plantedAcres > 0) sb.append("🌱 ").append(trim(s.plantedAcres)).append(I18n.t(" acres planted   "));
        if (s.platformSales > 0) sb.append("🛒 ").append(Ui.kes(s.platformSales)).append(I18n.t(" sold on FarmConnect   "));
        if (s.harvestByCrop != null) for (Map.Entry<String, Double> e : s.harvestByCrop.entrySet())
            sb.append("🌾 ").append(e.getKey()).append(" ").append(trim(e.getValue())).append("   ");
        if (s.livestock != null) for (Map.Entry<String, Double> e : s.livestock.entrySet())
            sb.append("🐄 ").append(e.getKey()).append(" ").append(trim(e.getValue())).append("   ");
        if (sb.length() > 0) {
            android.widget.TextView t = Cards.text(this, sb.toString().trim(), 13, 0xFF1C2B1E, false);
            t.setPadding(dp(8), dp(6), dp(8), dp(6));
            top.addView(t);
        }
    }

    @Override protected void onFab() { form(null); }

    private void form(FarmRecord ex) {
        if (farms.isEmpty()) { Ui.toast(this, I18n.t("Add a farm first (Dashboard → My farms)")); return; }
        String[] fIds = new String[farms.size()], fNames = new String[farms.size()];
        for (int i = 0; i < farms.size(); i++) { fIds[i] = String.valueOf(farms.get(i).id); fNames[i] = farms.get(i).name; }
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("farm", I18n.t("Farm"), true, fIds, fNames).value(ex != null ? String.valueOf(ex.farmId) : fIds[0]));
        f.add(Forms.choice("type", I18n.t("Type of record"), true, Labels.RECORD_TYPES, Labels.RECORD_NAMES)
                .value(ex != null ? ex.type : (chip.isEmpty() ? "HARVEST" : chip)));
        f.add(Forms.text("title", I18n.t("Crop, animal or item (e.g. Maize, Dairy cows, Fertiliser)"), true).value(ex == null ? "" : ex.title));
        f.add(Forms.date("date", I18n.t("Date"), false).value(ex == null ? "" : ex.date));
        f.add(Forms.number("quantity", I18n.t("Quantity (acres planted, kg harvested, number of animals)"), false)
                .value(ex == null || ex.quantity == null ? "" : trim(ex.quantity)));
        f.add(Forms.text("unit", I18n.t("Unit (acres, kg, bags, animals...)"), false).value(ex == null ? "" : ex.unit));
        f.add(Forms.number("amount", I18n.t("Amount in KES (for expenses and income)"), false)
                .value(ex == null || ex.amount == null ? "" : trim(ex.amount)));
        f.add(Forms.multi("notes", I18n.t("Notes (optional)"), false).value(ex == null ? "" : ex.notes));
        Forms.show(this, ex == null ? I18n.t("New record") : I18n.t("Edit record"), I18n.t("Save"), f, v -> {
            RecordRequest r = new RecordRequest();
            r.farmId = Long.parseLong(v.get("farm"));
            r.type = v.get("type");
            r.title = v.get("title");
            r.date = v.get("date").isEmpty() ? null : v.get("date");
            r.quantity = v.get("quantity").isEmpty() ? null : Double.valueOf(v.get("quantity"));
            r.unit = v.get("unit");
            r.amount = v.get("amount").isEmpty() ? null : Double.valueOf(v.get("amount"));
            r.notes = v.get("notes");
            Ui.watch(this, ex == null ? vm.createRecord(r) : vm.updateRecord(ex.id, r), b.progress, x -> {
                Ui.toast(this, I18n.t("Saved"));
                load();
            });
        });
    }
}

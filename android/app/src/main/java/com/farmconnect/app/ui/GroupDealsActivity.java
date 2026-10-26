package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.Deal;
import com.farmconnect.app.data.Models.DealDetail;
import com.farmconnect.app.data.Models.DealRequest;
import com.farmconnect.app.data.Models.DealShare;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Group deals. Members pool produce to sell in bulk, or pool orders for seed, fertilizer and tools to buy together.
 * Record only: the app tracks the quantities, the price and each member's share. Money is settled outside the app.
 * Extras: "groupId" (one group's deals), "manager" (may create), "market" (open produce deals, for buyers).
 */
public class GroupDealsActivity extends BaseListActivity {
    private long groupId() { return getIntent().getLongExtra("groupId", -1); }
    private boolean market() { return getIntent().getBooleanExtra("market", false); }
    private boolean manager() { return getIntent().getBooleanExtra("manager", false); }

    @Override protected String screenTitle() {
        return getIntent().getBooleanExtra("market", false) ? I18n.t("Group produce for sale") : I18n.t("Group deals");
    }
    @Override protected String emptyText() {
        return getIntent().getBooleanExtra("market", false) ? I18n.t("No group produce is open right now")
                : I18n.t("No deals yet. The group leader can start one.");
    }
    @Override protected String[][] chips() {
        return getIntent().getBooleanExtra("market", false) ? null
                : new String[][]{{"", "All"}, {"SELL", "Selling together"}, {"BUY", "Buying together"}};
    }
    @Override protected String fabText() {
        return getIntent().getLongExtra("groupId", -1) > 0 && getIntent().getBooleanExtra("manager", false) ? I18n.t("➕ New deal") : null;
    }
    @Override protected void onFab() { create(); }

    private static String qty(double q) {
        return q == Math.rint(q) ? String.valueOf((long) q) : String.format(Locale.US, "%.2f", q);
    }

    private static String statusName(String s) {
        if ("OPEN".equals(s)) return I18n.t("Open");
        if ("CLOSED".equals(s)) return I18n.t("Closed");
        if ("DONE".equals(s)) return I18n.t("Done");
        return I18n.t("Cancelled");
    }

    private static int statusColor(String s) {
        if ("OPEN".equals(s)) return 0xFF2E7D32;
        if ("CLOSED".equals(s)) return 0xFFF9A825;
        if ("DONE".equals(s)) return 0xFF1E88E5;
        return 0xFF757575;
    }

    private static String price(Deal d) {
        if (d.unitPrice == null) return I18n.t("Price not set");
        return Ui.kes(d.unitPrice) + " / " + d.unit + (d.discountPct == null ? "" : "  (−" + d.discountPct + "%)");
    }

    @Override protected void load() {
        if (vm == null) return;
        androidx.lifecycle.LiveData<com.farmconnect.app.data.Resource<List<Deal>>> src =
                market() ? vm.dealMarket() : vm.deals(groupId() > 0 ? groupId() : null, chip.isEmpty() ? null : chip);
        Ui.watch(this, src, b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (Deal d : list) {
                rows.add(Row.of("SELL".equals(d.kind) ? "🌾" : "🛒", d.item + " · " + qty(d.targetQty) + " " + d.unit)
                        .sub(d.groupName + " · " + qty(d.committedQty) + " " + I18n.t("of") + " " + qty(d.targetQty) + " (" + d.progressPct + "%)")
                        .meta(price(d) + (d.myQty == null ? "" : " · " + I18n.t("You: ") + qty(d.myQty)))
                        .badge(statusName(d.status), statusColor(d.status))
                        .click(() -> open(d.id)));
            }
            show(rows, list.size() + I18n.t(" deals"));
        });
    }

    private void open(long id) { Ui.watch(this, vm.deal(id), b.progress, this::showDeal); }

    private void showDeal(DealDetail dd) {
        Deal d = dd.pool;
        boolean open = "OPEN".equals(d.status);
        boolean sell = "SELL".equals(d.kind);
        Sheet s = Sheet.of(this, d.item).badge(statusName(d.status), statusColor(d.status))
                .field(I18n.t("Group"), d.groupName + (d.subCounty == null ? "" : " · " + d.subCounty))
                .field(sell ? I18n.t("Selling together") : I18n.t("Buying together"),
                        qty(d.committedQty) + " " + I18n.t("of") + " " + qty(d.targetQty) + " " + d.unit + " (" + d.progressPct + "%) · "
                                + d.members + (d.members == 1 ? I18n.t(" member") : I18n.t(" members")))
                .field(sell ? I18n.t("Asking price") : I18n.t("Group price"), d.unitPrice == null ? null : price(d))
                .field(I18n.t("Normal price"), d.listPrice == null ? null : Ui.kes(d.listPrice) + " / " + d.unit)
                .field(I18n.t("Closes"), d.closesAt == null ? null : Ui.dateOnly(d.closesAt))
                .note(d.notes);
        if (d.myQty != null) s.strong(I18n.t("Your share"), qty(d.myQty) + " " + d.unit + (d.myAmount == null ? "" : " = " + Ui.kes(d.myAmount)));
        if (d.finalNote != null && !d.finalNote.isEmpty()) s.field(I18n.t("Final note"), d.finalNote);
        if (dd.shares != null && !dd.shares.isEmpty() && (d.isMember || d.canManage)) {
            StringBuilder sb = new StringBuilder();
            for (DealShare x : dd.shares) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(x.name).append(": ").append(qty(x.qty)).append(" ").append(d.unit);
                if (x.amount != null) sb.append(" = ").append(Ui.kes(x.amount));
                if (d.canManage && x.phone != null) sb.append("  ").append(x.phone);
            }
            s.divider().field(I18n.t("Who is in"), sb.toString());
        }
        s.note(I18n.t("FarmConnect only keeps the record. Money is settled between members and the buyer or dealer."));
        if (open && d.isMember && "FARMER".equals(Session.role()))
            s.primary(d.myQty == null ? I18n.t(sell ? "Add my produce" : "Add my order") : I18n.t("Change my quantity"), () -> setShare(d));
        if (d.canManage && open) s.secondary(I18n.t("Stop taking quantities"), () -> act(d, vm.dealClose(d.id), I18n.t("Deal closed")));
        if (d.canManage && !"DONE".equals(d.status) && !"CANCELLED".equals(d.status)) {
            s.secondary(I18n.t("Mark as done"), () -> markDone(d));
            s.danger(I18n.t("Cancel this deal"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Cancel this deal?"))
                    .setMessage(I18n.t("Members who joined will be told."))
                    .setPositiveButton(I18n.t("Cancel deal"), (x, y) -> act(d, vm.dealCancel(d.id), I18n.t("Deal cancelled")))
                    .setNegativeButton(I18n.t("Keep"), null).show());
        }
        if (!d.isMember && !d.canManage && d.leaderId > 0 && d.leaderId != Session.id())
            s.secondary(I18n.t("💬 Message the group leader"), () -> startActivity(new Intent(this, ChatActivity.class)
                    .putExtra("userId", d.leaderId).putExtra("name", d.leaderName)));
        s.show();
    }

    private void act(Deal d, androidx.lifecycle.LiveData<com.farmconnect.app.data.Resource<DealDetail>> call, String done) {
        Ui.watch(this, call, b.progress, r -> { Ui.toast(this, done); load(); });
    }

    private void setShare(Deal d) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.number("qty", I18n.t("How many ") + d.unit + I18n.t(" (0 to leave the deal)"), true).value(d.myQty == null ? "" : qty(d.myQty)));
        Forms.show(this, d.item, I18n.t("Save"), f, v -> {
            double q;
            try { q = Double.parseDouble(v.get("qty")); } catch (Exception e) { Ui.toast(this, I18n.t("Enter a number")); return; }
            if (q < 0) { Ui.toast(this, I18n.t("Enter a number")); return; }
            Ui.watch(this, vm.dealShare(d.id, q), b.progress, r -> { Ui.toast(this, I18n.t("Saved")); load(); showDeal(r); });
        });
    }

    private void markDone(Deal d) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.number("price", I18n.t("Final price per unit (optional)"), false).value(d.unitPrice == null ? "" : qty(d.unitPrice)));
        f.add(Forms.multi("note", I18n.t("Note for members (optional)"), false));
        Forms.show(this, I18n.t("Mark as done"), I18n.t("Save"), f, v -> {
            Double p = null;
            try { if (!v.get("price").isEmpty()) p = Double.parseDouble(v.get("price")); } catch (Exception e) { Ui.toast(this, I18n.t("Enter a number")); return; }
            Ui.watch(this, vm.dealComplete(d.id, p, v.get("note")), b.progress, r -> { Ui.toast(this, I18n.t("Deal marked as done")); load(); });
        });
    }

    private void create() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("kind", I18n.t("What is this deal?"), true, Forms.arr("SELL", "BUY"),
                Forms.arr("Sell produce together", "Buy seeds, fertilizer or tools together")).value("SELL".equals(chip) || "BUY".equals(chip) ? chip : "SELL"));
        f.add(Forms.text("item", I18n.t("Produce or input (e.g. Maize, DAP fertilizer)"), true));
        f.add(Forms.text("unit", I18n.t("Unit (e.g. 90 kg bag, kg, litre)"), true));
        f.add(Forms.number("target", I18n.t("Target quantity"), true));
        f.add(Forms.number("price", I18n.t("Price per unit (optional)"), false));
        f.add(Forms.number("list", I18n.t("Normal price per unit, to show the saving (buying only, optional)"), false));
        f.add(Forms.date("closes", I18n.t("Last day to join (optional)"), false));
        f.add(Forms.multi("notes", I18n.t("Notes, e.g. buyer, dealer or pick-up place (optional)"), false));
        Forms.show(this, I18n.t("New group deal"), I18n.t("Start deal"), f, v -> {
            DealRequest r = new DealRequest();
            try {
                r.targetQty = Double.parseDouble(v.get("target"));
                if (!v.get("price").isEmpty()) r.unitPrice = Double.parseDouble(v.get("price"));
                if (!v.get("list").isEmpty()) r.listPrice = Double.parseDouble(v.get("list"));
            } catch (Exception e) { Ui.toast(this, I18n.t("Enter a number")); return; }
            r.groupId = groupId();
            r.kind = v.get("kind");
            r.item = v.get("item");
            r.unit = v.get("unit");
            r.closesAt = v.get("closes").isEmpty() ? null : v.get("closes");
            r.notes = v.get("notes");
            Ui.watch(this, vm.dealCreate(r), b.progress, res -> { Ui.toast(this, I18n.t("Deal started. Members were told.")); load(); });
        });
    }
}

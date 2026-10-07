package com.farmconnect.app.ui;

import android.content.Intent;
import com.farmconnect.app.data.Models.AppNotification;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/** The bell: order updates, verification results, messages, new programmes and group notices. */
public class NotificationsActivity extends BaseListActivity {
    @Override protected String screenTitle() { return "Alerts"; }
    @Override protected String emptyText() { return "You are all caught up 🎉\nOrder updates, messages and county notices appear here."; }
    @Override protected String headerActionLabel() { return "Mark all read"; }

    @Override protected void onHeaderAction() {
        Ui.watch(this, vm.readAllNotifications(), b.progress, c -> load());
    }

    private static String icon(String t) {
        switch (t == null ? "" : t) {
            case "ORDER": return "📦";
            case "VERIFICATION": return "✅";
            case "MESSAGE": return "💬";
            case "POST": return "📢";
            case "GROUP": return "👥";
            default: return "🔔";
        }
    }

    @Override protected void load() {
        Ui.watch(this, vm.notifications(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            int unread = 0;
            for (AppNotification n : list) {
                if (!n.read) unread++;
                rows.add(Row.of(icon(n.type), n.title).sub(n.body).meta(Ui.dateTime(n.createdAt)).unread(!n.read).click(() -> open(n)));
            }
            show(rows, unread > 0 ? unread + " unread" : "All read");
        });
    }

    private void open(AppNotification n) {
        if (!n.read) vm.readNotification(n.id).observe(this, r -> { });
        Intent i = null;
        switch (n.type == null ? "" : n.type) {
            case "MESSAGE": {
                String name = n.title != null && n.title.contains("from ") ? n.title.substring(n.title.indexOf("from ") + 5) : "Chat";
                i = new Intent(this, ChatActivity.class).putExtra("userId", n.refId).putExtra("name", name);
                break;
            }
            case "POST":
                i = new Intent(this, PostDetailActivity.class).putExtra("id", n.refId);
                break;
            case "GROUP":
                i = new Intent(this, GroupDetailActivity.class).putExtra("id", n.refId);
                break;
            case "ORDER": {
                String role = Session.role();
                String mode = "FARMER".equals(role) ? "FARMER" : "ADMIN".equals(role) ? "ADMIN" : "BUYER";
                i = new Intent(this, OrdersActivity.class).putExtra("mode", mode);
                break;
            }
            default:
                break;
        }
        if (i != null) startActivity(i);
        else load();
    }
}

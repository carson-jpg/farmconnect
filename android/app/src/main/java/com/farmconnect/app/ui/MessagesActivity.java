package com.farmconnect.app.ui;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.Contact;
import com.farmconnect.app.data.Models.Conversation;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/** Inbox: one row per person you have talked to. */
public class MessagesActivity extends BaseListActivity {
    @Override protected String screenTitle() { return "Messages"; }
    @Override protected String emptyText() { return "No conversations yet.\nTap “New message” to talk to a seller, buyer or county officer."; }
    @Override protected String fabText() { return "New message"; }

    @Override protected void load() {
        Ui.watch(this, vm.conversations(), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            long unreadTotal = 0;
            for (Conversation c : list) {
                unreadTotal += c.unread;
                Row r = Row.of(Labels.roleIcon(c.role), c.name + (c.verified ? "  ✓" : ""))
                        .sub(c.lastMessage)
                        .meta(Labels.roleName(c.role) + " · " + Ui.dateTime(c.lastAt))
                        .unread(c.unread > 0)
                        .click(() -> open(c.userId, c.name));
                if (c.unread > 0) r.badge(c.unread + " new", 0xFFC62828);
                rows.add(r);
            }
            show(rows, unreadTotal > 0 ? unreadTotal + " unread" : list.size() + " conversations");
        });
    }

    private void open(long userId, String name) {
        startActivity(new Intent(this, ChatActivity.class).putExtra("userId", userId).putExtra("name", name));
    }

    @Override protected void onFab() {
        Ui.watch(this, vm.contacts(""), b.progress, list -> {
            if (list.isEmpty()) { Ui.toast(this, "There is nobody you can message yet"); return; }
            String[] names = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Contact c = list.get(i);
                names[i] = Labels.roleIcon(c.role) + "  " + c.name + "  ·  " + Labels.roleName(c.role) + (c.verified ? " ✓" : "");
            }
            new AlertDialog.Builder(this).setTitle("Message who?").setItems(names, (d, i) -> open(list.get(i).id, list.get(i).name)).show();
        });
    }
}

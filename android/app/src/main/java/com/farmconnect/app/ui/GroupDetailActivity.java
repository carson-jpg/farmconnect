package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.Group;
import com.farmconnect.app.data.Models.GroupDetail;
import com.farmconnect.app.data.Models.Member;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

/** One group: details, members, join / leave, notices to members, message the leader. */
public class GroupDetailActivity extends BaseListActivity {
    private long groupId;

    @Override protected String screenTitle() { return I18n.t("Group"); }
    @Override protected String emptyText() { return I18n.t("No members yet"); }

    @Override protected void load() {
        groupId = getIntent().getLongExtra("id", -1);
        if (groupId < 0) { finish(); return; }
        Ui.watch(this, vm.group(groupId), b.progress, this::render);
    }

    private MaterialButton button(String text, boolean outlined, View.OnClickListener l) {
        MaterialButton btn = new MaterialButton(this, null,
                outlined ? com.google.android.material.R.attr.materialButtonOutlinedStyle : com.google.android.material.R.attr.materialButtonStyle);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setCornerRadius(dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        lp.topMargin = dp(8);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(l);
        return btn;
    }

    private void render(GroupDetail d) {
        Group g = d.group;
        boolean staff = Labels.isStaff();
        boolean manager = staff || g.leader;
        long me = Session.id();
        b.tvTitle.setText(g.name);

        LinearLayout top = b.topArea;
        top.removeAllViews();
        top.setPadding(dp(14), dp(10), dp(14), 0);
        MaterialCardView card = Cards.card(this, top, 0);
        LinearLayout in = Cards.inner(card);
        in.addView(Cards.text(this, Labels.groupIcon(g.type) + "  " + Labels.name(Labels.GROUP_TYPES, Labels.GROUP_NAMES, g.type), 13, 0xFF2E7D32, true));
        if (g.description != null && !g.description.isEmpty()) {
            TextView t = Cards.text(this, g.description, 14, 0xFF1C2B1E, false);
            t.setPadding(0, dp(8), 0, 0);
            in.addView(t);
        }
        StringBuilder info = new StringBuilder("👥 " + g.members + (g.members == 1 ? I18n.t(" member") : I18n.t(" members")));
        if (g.leaderName != null) info.append(I18n.t("\n⭐ Led by ")).append(g.leaderName);
        if (g.location != null && !g.location.isEmpty()) info.append("\n📍 ").append(g.location);
        if (g.subCounty != null && !g.subCounty.isEmpty()) info.append(", ").append(g.subCounty);
        if (g.contactPhone != null && !g.contactPhone.isEmpty()) info.append("\n📞 ").append(g.contactPhone);
        TextView it = Cards.text(this, info.toString(), 13, 0xFF5E7061, false);
        it.setPadding(0, dp(10), 0, 0);
        in.addView(it);

        if ("FARMER".equals(Session.role())) {
            if (!g.member) in.addView(button(I18n.t("Join this group"), false, v ->
                    Ui.watch(this, vm.joinGroup(g.id), b.progress, x -> { Ui.toast(this, I18n.t("Welcome to ") + g.name); load(); })));
            else if (!g.leader) in.addView(button(I18n.t("Leave group"), true, v -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Leave ") + g.name + "?")
                    .setPositiveButton(I18n.t("Leave"), (x, y) -> Ui.watch(this, vm.leaveGroup(g.id), b.progress, r -> { load(); }))
                    .setNegativeButton(I18n.t("Stay"), null).show()));
        }
        if (g.leaderId != me && g.leaderId > 0) in.addView(button(I18n.t("💬 Message the leader"), true, v ->
                startActivity(new Intent(this, ChatActivity.class).putExtra("userId", g.leaderId).putExtra("name", g.leaderName))));
        if (manager) {
            in.addView(button(I18n.t("📣 Send a notice to all members"), true, v -> {
                List<Forms.Field> f = new ArrayList<>();
                f.add(Forms.text("title", I18n.t("Title"), true));
                f.add(Forms.multi("body", I18n.t("Message"), true));
                Forms.show(this, I18n.t("Notice to members"), I18n.t("Send"), f, val ->
                        Ui.watch(this, vm.announceToGroup(g.id, val.get("title"), val.get("body")), b.progress,
                                r -> Ui.toast(this, I18n.t("Sent to ") + (g.members - 1) + I18n.t(" members"))));
            }));
            in.addView(button(I18n.t("🗑 Delete group"), true, v -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Delete ") + g.name + "?")
                    .setMessage(I18n.t("All memberships are removed. This cannot be undone."))
                    .setPositiveButton(I18n.t("Delete"), (x, y) -> Ui.watch(this, vm.deleteGroup(g.id), b.progress, r -> {
                        Ui.toast(this, I18n.t("Group deleted"));
                        finish();
                    }))
                    .setNegativeButton(I18n.t("Cancel"), null).show()));
        }

        List<Row> rows = new ArrayList<>();
        for (Member m : d.members) {
            Row r = Row.of(m.leader ? "⭐" : "🧑‍🌾", m.name + (m.verified ? "  ✓" : ""))
                    .sub(m.phone)
                    .meta(I18n.t("Joined ") + Ui.dateOnly(m.joinedAt));
            if (m.leader) r.badge(I18n.t("Leader"), 0xFFB26A00);
            if (m.userId != me) r.click(() -> startActivity(new Intent(this, ChatActivity.class)
                    .putExtra("userId", m.userId).putExtra("name", m.name)));
            if (manager && !m.leader && m.userId != me)
                r.action(I18n.t("Remove"), () -> new AlertDialog.Builder(this)
                        .setTitle(I18n.t("Remove ") + m.name + "?")
                        .setPositiveButton(I18n.t("Remove"), (x, y) -> Ui.watch(this, vm.removeGroupMember(g.id, m.userId), b.progress, z -> load()))
                        .setNegativeButton(I18n.t("Cancel"), null).show());
            rows.add(r);
        }
        show(rows, g.members + (g.members == 1 ? I18n.t(" member") : I18n.t(" members")));
    }
}

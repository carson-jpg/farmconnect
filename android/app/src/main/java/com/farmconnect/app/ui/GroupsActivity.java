package com.farmconnect.app.ui;

import android.content.Intent;
import com.farmconnect.app.data.Models.Group;
import com.farmconnect.app.data.Models.GroupRequest;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/** Farmer groups, cooperatives and SACCOs: browse, join, create. */
public class GroupsActivity extends BaseListActivity {
    @Override protected String screenTitle() { return "Groups & cooperatives"; }
    @Override protected boolean searchable() { return true; }
    @Override protected String searchHint() { return "Search groups..."; }
    @Override protected String fabText() { return "BUYER".equals(Session.role()) ? null : "Create group"; }
    @Override protected String emptyText() { return "No groups here yet.\nCreate one and invite your neighbours to join."; }

    @Override protected String[][] chips() {
        String[][] c = new String[Labels.GROUP_TYPES.length + 2][];
        c[0] = new String[]{"", "All"};
        c[1] = new String[]{"MINE", "My groups"};
        for (int i = 0; i < Labels.GROUP_TYPES.length; i++) c[i + 2] = new String[]{Labels.GROUP_TYPES[i], Labels.GROUP_NAMES[i]};
        return c;
    }

    @Override protected void load() {
        boolean mine = "MINE".equals(chip);
        Ui.watch(this, vm.groups(mine, mine ? "" : chip, query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            boolean farmer = "FARMER".equals(Session.role());
            for (Group g : list) {
                String meta = "👥 " + g.members + (g.members == 1 ? " member" : " members")
                        + (g.subCounty == null || g.subCounty.isEmpty() ? "" : " · " + g.subCounty)
                        + (g.leaderName == null ? "" : " · Led by " + g.leaderName);
                Row r = Row.of(Labels.groupIcon(g.type), g.name).sub(g.description).meta(meta)
                        .click(() -> startActivity(new Intent(this, GroupDetailActivity.class).putExtra("id", g.id)));
                if (g.leader) r.badge("Leader", 0xFFB26A00);
                else if (g.member) r.badge("Member");
                else r.badge(Labels.name(Labels.GROUP_TYPES, Labels.GROUP_NAMES, g.type));
                if (farmer && !g.member) r.action("Join", () -> Ui.watch(this, vm.joinGroup(g.id), b.progress, x -> {
                    Ui.toast(this, "You joined " + g.name);
                    load();
                }));
                rows.add(r);
            }
            show(rows, list.size() + (list.size() == 1 ? " group" : " groups"));
        });
    }

    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.text("name", "Group name", true));
        f.add(Forms.choice("type", "Type", true, Labels.GROUP_TYPES, Labels.GROUP_NAMES).value("FARMER_GROUP"));
        f.add(Forms.multi("description", "What does the group do?", false));
        f.add(Forms.choice("subCounty", "Sub-county", false, Labels.SUB_COUNTIES, Labels.SUB_COUNTIES));
        f.add(Forms.text("location", "Meeting place / town", false));
        f.add(Forms.phone("phone", "Contact phone", false));
        Forms.show(this, "New group", "Create", f, v -> {
            GroupRequest r = new GroupRequest();
            r.name = v.get("name");
            r.type = v.get("type");
            r.description = v.get("description");
            r.subCounty = v.get("subCounty");
            r.location = v.get("location");
            r.contactPhone = v.get("phone");
            Ui.watch(this, vm.createGroup(r), b.progress, g -> {
                Ui.toast(this, "Group created");
                load();
            });
        });
    }
}

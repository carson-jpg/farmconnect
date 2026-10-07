package com.farmconnect.app.ui;

import android.content.Intent;
import com.farmconnect.app.data.Models.Post;
import com.farmconnect.app.data.Models.PostRequest;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/** Learn & opportunities: farming tips, articles, advisories, announcements, county programmes, trainings. */
public class InfoHubActivity extends BaseListActivity {
    @Override protected String screenTitle() { return "Learn & opportunities"; }
    @Override protected boolean searchable() { return true; }
    @Override protected String searchHint() { return "Search tips, trainings, programmes..."; }
    @Override protected String emptyText() { return "Nothing published here yet.\nCounty officers post tips, trainings and programmes in this space."; }
    @Override protected String fabText() { return Labels.isStaff() ? "Publish" : null; }

    @Override protected String[][] chips() {
        return new String[][]{{"", "All"}, {"TIP", "Tips"}, {"ARTICLE", "Articles"}, {"ADVISORY", "Advisories"},
                {"ANNOUNCEMENT", "Announcements"}, {"PROGRAMME", "Programmes"}, {"TRAINING", "Trainings"}, {"OPPORTUNITY", "Opportunities"}};
    }

    @Override protected void load() {
        Ui.watch(this, vm.posts(chip, query()), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (Post p : list) {
                StringBuilder meta = new StringBuilder(p.authorName == null ? "County" : p.authorName);
                meta.append(" · ").append(Ui.dateOnly(p.createdAt)).append(" · 👁 ").append(p.views);
                if (p.eventDate != null) meta.append("\n📅 ").append(Ui.dateOnly(p.eventDate));
                if (p.location != null && !p.location.isEmpty()) meta.append(" · 📍 ").append(p.location);
                rows.add(Row.of(Labels.postIcon(p.type), p.title).sub(p.excerpt).meta(meta.toString())
                        .badge(Labels.name(Labels.POST_TYPES, Labels.POST_NAMES, p.type))
                        .click(() -> startActivity(new Intent(this, PostDetailActivity.class).putExtra("id", p.id))));
            }
            show(rows, list.size() + (list.size() == 1 ? " item" : " items"));
        });
    }

    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("type", "Type", true, Labels.POST_TYPES, Labels.POST_NAMES).value("ANNOUNCEMENT"));
        f.add(Forms.text("title", "Title", true));
        f.add(Forms.choice("topic", "Topic", false, Labels.TOPICS, Labels.TOPIC_NAMES).value("GENERAL"));
        f.add(Forms.multi("body", "Message / details", true));
        f.add(Forms.date("date", "Date (training, programme or deadline)", false));
        f.add(Forms.text("location", "Venue / location", false));
        f.add(Forms.text("contact", "Contact phone or email", false));
        f.add(Forms.choice("audience", "Who should see it?", true, Forms.arr("ALL", "FARMERS", "BUYERS"),
                Forms.arr("Everyone", "Farmers only", "Buyers only")).value("ALL"));
        f.add(Forms.choice("sms", "Also send an SMS?", true, Forms.arr("NO", "YES"), Forms.arr("No, in-app only", "Yes, SMS too")).value("NO"));
        Forms.show(this, "Publish to farmers", "Publish", f, v -> {
            PostRequest r = new PostRequest();
            r.type = v.get("type");
            r.title = v.get("title");
            r.topic = v.get("topic");
            r.body = v.get("body");
            r.location = v.get("location");
            r.contact = v.get("contact");
            r.audience = v.get("audience");
            r.eventDate = v.get("date").isEmpty() ? null : v.get("date") + "T12:00:00Z";
            r.notifyUsers = true;
            r.sendSms = "YES".equals(v.get("sms"));
            Ui.watch(this, vm.createPost(r), b.progress, p -> {
                Ui.toast(this, "Published and everyone was notified");
                load();
            });
        });
    }
}

package com.farmconnect.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.R;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The row of quick links on every dashboard (Learn, Weather, Records, Groups, Messages, Alerts...) with unread badges. */
public final class Hub {
    private final AppCompatActivity a;
    private final CommunityViewModel vm;
    private TextView badgeAlerts, badgeMessages;

    private Hub(AppCompatActivity a) {
        this.a = a;
        this.vm = new ViewModelProvider(a).get(CommunityViewModel.class);
    }

    private static class Link {
        final String icon, label; final Runnable go; final String badge;
        Link(String icon, String label, String badge, Runnable go) { this.icon = icon; this.label = label; this.badge = badge; this.go = go; }
    }

    public static Hub attach(AppCompatActivity a, LinearLayout container) {
        Hub h = new Hub(a);
        h.build(container);
        return h;
    }

    private Runnable open(Class<?> c) { return () -> a.startActivity(new Intent(a, c)); }

    private void build(LinearLayout container) {
        String role = Session.role() == null ? "" : Session.role();
        boolean staff = role.equals("ADMIN") || role.equals("OFFICER");
        List<Link> links = new ArrayList<>();
        if (role.equals("FARMER")) {
            links.addAll(Arrays.asList(
                    new Link("📚", "Learn", null, open(InfoHubActivity.class)),
                    new Link("🌦", "Weather", null, open(WeatherActivity.class)),
                    new Link("📒", "Records", null, open(RecordsActivity.class)),
                    new Link("👥", "Groups", null, open(GroupsActivity.class)),
                    new Link("🧰", "Services", null, open(ServicesActivity.class))));
        } else if (staff) {
            links.addAll(Arrays.asList(
                    new Link("📢", "Publish", null, open(InfoHubActivity.class)),
                    new Link("📊", "Analytics", null, open(AnalyticsActivity.class)),
                    new Link("👥", "Groups", null, open(GroupsActivity.class)),
                    new Link("🧰", "Directory", null, open(ServicesActivity.class)),
                    new Link("🌦", "Weather", null, open(WeatherActivity.class))));
        } else {
            links.addAll(Arrays.asList(
                    new Link("📚", "Learn", null, open(InfoHubActivity.class)),
                    new Link("🧰", "Services", null, open(ServicesActivity.class)),
                    new Link("🌦", "Weather", null, open(WeatherActivity.class))));
        }
        links.add(new Link("💬", "Messages", "m", open(MessagesActivity.class)));
        links.add(new Link("🔔", "Alerts", "n", open(NotificationsActivity.class)));
        if (!staff) links.add(new Link("⭐", "Rate us", null, this::rateDialog));

        container.removeAllViews();
        for (Link l : links) container.addView(tile(l));
    }

    private View tile(Link l) {
        Context c = a;
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Cards.dp(c, 74), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Cards.dp(c, 3), 0, Cards.dp(c, 3), 0);
        col.setLayoutParams(lp);
        col.setPadding(0, Cards.dp(c, 4), 0, Cards.dp(c, 4));

        FrameLayout holder = new FrameLayout(c);
        TextView icon = new TextView(c);
        icon.setText(l.icon);
        icon.setTextSize(24);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.bg_icon_circle);
        holder.addView(icon, new FrameLayout.LayoutParams(Cards.dp(c, 56), Cards.dp(c, 56), Gravity.CENTER));
        if (l.badge != null) {
            TextView badge = new TextView(c);
            badge.setTextColor(0xFFFFFFFF);
            badge.setTextSize(10);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setMinWidth(Cards.dp(c, 18));
            badge.setPadding(Cards.dp(c, 5), 0, Cards.dp(c, 5), 0);
            badge.setBackgroundResource(R.drawable.bg_dot_cancel);
            badge.setVisibility(View.GONE);
            FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Cards.dp(c, 18),
                    Gravity.TOP | Gravity.END);
            holder.addView(badge, bp);
            if (l.badge.equals("n")) badgeAlerts = badge; else badgeMessages = badge;
        }
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(Cards.dp(c, 62), Cards.dp(c, 58));
        col.addView(holder, new LinearLayout.LayoutParams(Cards.dp(c, 62), Cards.dp(c, 58)));

        TextView label = new TextView(c);
        label.setText(l.label);
        label.setTextSize(11);
        label.setTextColor(0xFF1C2B1E);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        col.addView(label);
        col.setOnClickListener(v -> l.go.run());
        return col;
    }

    /** Re-reads the unread counts. Call from onResume of the dashboard. */
    public void refresh() {
        vm.counts().observe(a, res -> {
            if (res.status != Resource.Status.SUCCESS || res.data == null) return;
            setBadge(badgeAlerts, res.data.notifications);
            setBadge(badgeMessages, res.data.messages);
        });
    }

    private static void setBadge(TextView t, long n) {
        if (t == null) return;
        t.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        t.setText(n > 99 ? "99+" : String.valueOf(n));
    }

    private void rateDialog() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("rating", "How do you like FarmConnect?", true, Forms.arr("5", "4", "3", "2", "1"),
                Forms.arr("★★★★★  Excellent", "★★★★  Good", "★★★  Okay", "★★  Poor", "★  Bad")));
        f.add(Forms.multi("comment", "Tell us what to improve (optional)", false));
        Forms.show(a, "Rate FarmConnect", "Send", f, v ->
                Ui.watch(a, vm.sendFeedback(Integer.parseInt(v.get("rating")), v.get("comment")), null,
                        x -> Ui.toast(a, "Thank you for your feedback!")));
    }
}

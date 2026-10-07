package com.farmconnect.app.ui;

/** One line in a generic list (posts, services, groups, messages, notifications, records, members...). */
public class Row {
    public String icon = "•", title = "", subtitle, meta, badge, action;
    public int badgeColor = 0xFF2E7D32;
    public boolean unread;
    public Runnable onClick, onAction;

    public static Row of(String icon, String title) { Row r = new Row(); r.icon = icon; r.title = title; return r; }
    public Row sub(String s) { subtitle = s; return this; }
    public Row meta(String s) { meta = s; return this; }
    public Row badge(String s, int color) { badge = s; badgeColor = color; return this; }
    public Row badge(String s) { badge = s; return this; }
    public Row unread(boolean u) { unread = u; return this; }
    public Row click(Runnable r) { onClick = r; return this; }
    public Row action(String label, Runnable r) { action = label; onAction = r; return this; }
}

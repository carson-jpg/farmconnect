package com.farmconnect.app.data.offline;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** The changes made without a network, in the order they were made, waiting to be sent. No Android code. */
public final class OfflineQueueModel {
    public static class Item {
        public long seq, userId, createdAt;
        public String method, path, body;
        /** For a record created offline: its temporary (negative) id. */
        public Long tempId;
    }

    private static final Gson GSON = new Gson();
    private final List<Item> items = new ArrayList<>();
    private long nextSeq = 1;

    public synchronized Item add(long userId, String method, String path, String body, Long tempId, long now) {
        Item i = new Item();
        i.seq = nextSeq++;
        i.userId = userId;
        i.method = method;
        i.path = path;
        i.body = body;
        i.tempId = tempId;
        i.createdAt = now;
        items.add(i);
        return i;
    }

    /** The user edited a record that is still waiting to be created: change what will be created instead of adding a second change. */
    public synchronized boolean absorbEdit(long userId, long tempId, String newBody) {
        for (Item i : items) if (i.userId == userId && "POST".equals(i.method) && i.tempId != null && i.tempId == tempId) { i.body = newBody; return true; }
        return false;
    }

    /** The user deleted a record that was never created: just forget it. */
    public synchronized boolean absorbDelete(long userId, long tempId) {
        for (Iterator<Item> it = items.iterator(); it.hasNext(); ) {
            Item i = it.next();
            if (i.userId == userId && "POST".equals(i.method) && i.tempId != null && i.tempId == tempId) { it.remove(); return true; }
        }
        return false;
    }

    public synchronized List<Item> pending(long userId) {
        List<Item> out = new ArrayList<>();
        for (Item i : items) if (i.userId == userId) out.add(i);
        out.sort((a, b) -> Long.compare(a.seq, b.seq));
        return out;
    }

    public synchronized int count(long userId) {
        int n = 0;
        for (Item i : items) if (i.userId == userId) n++;
        return n;
    }

    public synchronized void remove(long seq) { items.removeIf(i -> i.seq == seq); }

    public synchronized String toJson() { return GSON.toJson(new ArrayList<>(items)); }

    public static OfflineQueueModel fromJson(String json) {
        OfflineQueueModel m = new OfflineQueueModel();
        if (json == null || json.isEmpty()) return m;
        try {
            List<Item> list = GSON.fromJson(json, new TypeToken<List<Item>>() { }.getType());
            if (list != null) for (Item i : list) { m.items.add(i); m.nextSeq = Math.max(m.nextSeq, i.seq + 1); }
        } catch (Exception ignored) { }
        return m;
    }
}

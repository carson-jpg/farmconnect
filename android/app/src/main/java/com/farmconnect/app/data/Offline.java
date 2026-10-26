package com.farmconnect.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.farmconnect.app.data.offline.OfflineQueueModel;
import com.farmconnect.app.data.offline.OfflineRules;
import com.farmconnect.app.data.offline.RecordCachePatcher;
import com.farmconnect.app.util.I18n;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Offline mode. Everything the farmer has read is saved on the phone (per user, private app storage, cleared at log out),
 * so screens still open without a network. Farm records can also be added, edited and deleted offline: the changes wait
 * on the phone and are sent when the network returns.
 */
public final class Offline {
    private Offline() {}

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final long MAX_BODY = 2L * 1024 * 1024;
    private static final long MAX_CACHE = 25L * 1024 * 1024;
    private static final AtomicLong TEMP = new AtomicLong(-System.currentTimeMillis());
    private static final AtomicBoolean FLUSHING = new AtomicBoolean(false);
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build();

    private static Context app;
    private static File dir;
    private static SharedPreferences prefs;
    private static OfflineQueueModel queue = new OfflineQueueModel();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static long lastCacheToast;

    public static void init(Context c) {
        app = c.getApplicationContext();
        dir = new File(app.getFilesDir(), "offline");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        prefs = app.getSharedPreferences("farmconnect_offline", Context.MODE_PRIVATE);
        queue = OfflineQueueModel.fromJson(prefs.getString("queue", null));
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network n) { flush(); }
            });
        } catch (Exception ignored) { }
        flush();
    }

    public static boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            NetworkCapabilities caps = n == null ? null : cm.getNetworkCapabilities(n);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true;
        }
    }

    /** True when a failed call is because there is no network (not because the server was slow, which could mean it did get the request). */
    public static boolean looksOffline(IOException e) {
        return !isOnline() || e instanceof UnknownHostException || e instanceof ConnectException || e instanceof NoRouteToHostException;
    }

    public static int pending() { return Session.isLoggedIn() ? queue.count(Session.id()) : 0; }

    // ------------------------------------------------------------------ saved copies of what was read

    private static String keyOf(Request r) {
        String q = r.url().encodedQuery();
        return r.url().encodedPath() + (q == null || q.isEmpty() ? "" : "?" + q);
    }

    private static File fileFor(long uid, String key) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(uid + "_");
            for (byte b : h) sb.append(String.format(Locale.US, "%02x", b));
            return new File(dir, sb.append(".json").toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void cachePut(long uid, Request req, byte[] body) {
        if (dir == null || body.length > MAX_BODY) return;
        try {
            writeEntry(fileFor(uid, keyOf(req)), keyOf(req), System.currentTimeMillis(), body);
            trim();
        } catch (Exception ignored) { }
    }

    private static void writeEntry(File f, String key, long ts, byte[] body) throws IOException {
        byte[] head = (key + "\t" + ts + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[head.length + body.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(body, 0, all, head.length, body.length);
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(all); }
    }

    static class Entry { String key; long ts; byte[] body; }

    private static Entry read(File f) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream in = new FileInputStream(f)) {
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
            }
            byte[] all = buf.toByteArray();
            int nl = -1;
            for (int i = 0; i < all.length; i++) if (all[i] == '\n') { nl = i; break; }
            if (nl < 0) return null;
            String[] head = new String(all, 0, nl, StandardCharsets.UTF_8).split("\t");
            Entry e = new Entry();
            e.key = head[0];
            e.ts = Long.parseLong(head[1]);
            e.body = java.util.Arrays.copyOfRange(all, nl + 1, all.length);
            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    static Entry cacheGet(long uid, Request req) {
        if (dir == null) return null;
        File f = fileFor(uid, keyOf(req));
        return f.exists() ? read(f) : null;
    }

    private static void trim() {
        File[] files = dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File f : files) total += f.length();
        if (total <= MAX_CACHE) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : files) {
            if (total <= MAX_CACHE * 8 / 10) break;
            long len = f.length();
            if (f.delete()) total -= len;
        }
    }

    /** At log out: the saved copies go (they belong to the person who was signed in). Unsent changes stay on the phone for their owner. */
    public static void clearCache() {
        if (dir == null) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    static void noteServedFromCache(long ts) {
        long now = System.currentTimeMillis();
        if (now - lastCacheToast < 30_000) return;
        lastCacheToast = now;
        toast(I18n.t("📴 No internet. Showing information saved on your phone from ")
                + new SimpleDateFormat("dd MMM, HH:mm", I18n.locale()).format(new Date(ts)));
    }

    static Response fromCache(Request req, Entry e) {
        return new Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK (saved on phone)")
                .header("X-From-Cache", String.valueOf(e.ts)).body(ResponseBody.create(e.body, JSON)).build();
    }

    static Entry entryFor(long uid, Request req) { return cacheGet(uid, req); }

    // ------------------------------------------------------------------ changes made offline

    private static String bodyOf(Request req) throws IOException {
        if (req.body() == null) return null;
        Buffer b = new Buffer();
        req.body().writeTo(b);
        return b.readUtf8();
    }

    private static Response json(Request req, int code, String body) {
        return new Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message("OK (waiting to be sent)")
                .body(ResponseBody.create(body, JSON)).build();
    }

    private static String farmName(long uid, long farmId) {
        try {
            File f = fileFor(uid, "/api/farms/mine");
            Entry e = f.exists() ? read(f) : null;
            if (e == null) return "";
            for (JsonElement x : JsonParser.parseString(new String(e.body, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject o = x.getAsJsonObject();
                if (o.has("id") && o.get("id").getAsLong() == farmId && o.has("name")) return o.get("name").getAsString();
            }
        } catch (Exception ignored) { }
        return "";
    }

    /** Apply a change to every saved copy of the records list (the "All" list and each filter). */
    private static JsonObject baseRecord(long uid, long id) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (!f.getName().startsWith(uid + "_")) continue;
            Entry e = read(f);
            if (e == null || !isRecordList(e.key)) continue;
            try {
                JsonObject r = RecordCachePatcher.find(new String(e.body, StandardCharsets.UTF_8), id);
                if (r != null) return r;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static boolean isRecordList(String key) { return key.equals("/api/records") || key.startsWith("/api/records?"); }

    private interface ListEdit { String apply(String listJson, String listType); }

    private static void patchLists(long uid, ListEdit edit) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.getName().startsWith(uid + "_")) continue;
            Entry e = read(f);
            if (e == null || !isRecordList(e.key)) continue;
            try {
                int q = e.key.indexOf('?');
                String type = q < 0 ? null : OfflineRules.queryParam(e.key.substring(q + 1), "type");
                String patched = edit.apply(new String(e.body, StandardCharsets.UTF_8), type);
                writeEntry(f, e.key, e.ts, patched.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) { }
        }
    }

    private static void saveQueue() { prefs.edit().putString("queue", queue.toJson()).apply(); }

    /** Hold a record change on the phone and answer the app as if it worked. */
    static synchronized Response queueWrite(Request req, long uid) throws IOException {
        String method = req.method(), path = req.url().encodedPath(), body = bodyOf(req);
        Long id = OfflineRules.recordId(path);
        long now = System.currentTimeMillis();
        Response out;
        if ("POST".equals(method)) {
            long temp = TEMP.decrementAndGet();
            queue.add(uid, "POST", path, body, temp, now);
            long farmId = JsonParser.parseString(body).getAsJsonObject().has("farmId") ? JsonParser.parseString(body).getAsJsonObject().get("farmId").getAsLong() : -1;
            JsonObject rec = RecordCachePatcher.synthRecord(body, temp, farmName(uid, farmId), null);
            patchLists(uid, (list, type) -> RecordCachePatcher.add(list, type, rec));
            out = json(req, 201, rec.toString());
        } else if ("PUT".equals(method)) {
            JsonObject base = baseRecord(uid, id);
            JsonObject rec = RecordCachePatcher.synthRecord(body, id, "", base);
            if (OfflineRules.isTempId(id)) {
                if (!queue.absorbEdit(uid, id, body)) return json(req, 404, "{\"message\":\"" + I18n.t("That record is no longer available. Refresh the list.") + "\"}");
            } else {
                queue.add(uid, "PUT", path, body, null, now);
            }
            final long rid = id;
            patchLists(uid, (list, type) -> RecordCachePatcher.replace(list, type, rid, rec));
            out = json(req, 200, rec.toString());
        } else {
            if (OfflineRules.isTempId(id)) {
                if (!queue.absorbDelete(uid, id)) return json(req, 404, "{\"message\":\"" + I18n.t("That record is no longer available. Refresh the list.") + "\"}");
            } else {
                queue.add(uid, "DELETE", path, null, null, now);
            }
            final long rid = id;
            patchLists(uid, (list, type) -> RecordCachePatcher.remove(list, rid));
            out = json(req, 200, "");
        }
        saveQueue();
        toast(I18n.t("📴 Saved on your phone. It will be sent when you are back online."));
        return out;
    }

    // ------------------------------------------------------------------ sending what waited

    /** Temporary id -> real id for records that were created offline and have now been sent. */
    static Long mappedId(long uid, long temp) {
        try {
            JsonObject m = JsonParser.parseString(prefs.getString("idmap", "{}")).getAsJsonObject();
            String k = uid + ":" + temp;
            return m.has(k) ? m.get(k).getAsLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void saveMapping(long uid, long temp, long real) {
        try {
            JsonObject m = JsonParser.parseString(prefs.getString("idmap", "{}")).getAsJsonObject();
            if (m.size() > 200) m = new JsonObject();
            m.addProperty(uid + ":" + temp, real);
            prefs.edit().putString("idmap", m.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static void flush() {
        if (app == null || !Session.isLoggedIn() || queue.count(Session.id()) == 0) return;
        if (!FLUSHING.compareAndSet(false, true)) return;
        new Thread(() -> {
            int sent = 0, dropped = 0;
            try {
                long uid = Session.id();
                for (OfflineQueueModel.Item i : queue.pending(uid)) {
                    Request.Builder rb = new Request.Builder().url(ApiClient.absolute(i.path))
                            .header("Authorization", "Bearer " + Session.token());
                    rb.method(i.method, "DELETE".equals(i.method) ? null : RequestBody.create(i.body == null ? "" : i.body, JSON));
                    try (Response r = CLIENT.newCall(rb.build()).execute()) {
                        if (r.isSuccessful()) {
                            if ("POST".equals(i.method) && i.tempId != null && r.body() != null) {
                                try {
                                    saveMapping(uid, i.tempId, JsonParser.parseString(r.body().string()).getAsJsonObject().get("id").getAsLong());
                                } catch (Exception ignored) { }
                            }
                            queue.remove(i.seq);
                            sent++;
                        } else if (r.code() == 401 || r.code() >= 500) {
                            break;
                        } else {
                            queue.remove(i.seq);
                            dropped++;
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
                saveQueue();
            } finally {
                FLUSHING.set(false);
            }
            if (sent > 0) toast(I18n.t("✅ Sent the changes saved on your phone: ") + sent);
            if (dropped > 0) toast(I18n.t("⚠ Some saved changes were refused by the server and removed: ") + dropped);
        }).start();
    }

    private static void toast(String msg) {
        if (app == null) return;
        MAIN.post(() -> Toast.makeText(app, msg, Toast.LENGTH_LONG).show());
    }
}

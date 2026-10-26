package com.farmconnect.app.data.offline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Keeps the saved copy of the farm-records list in step with changes made offline, so the screen shows them at once.
 * listType is the filter the saved list was fetched with (null = all records). No Android code.
 */
public final class RecordCachePatcher {
    private RecordCachePatcher() {}

    /** The record the phone shows for a change that is still waiting to be sent. */
    public static JsonObject synthRecord(String requestBody, long id, String farmName, JsonObject base) {
        JsonObject rec = base == null ? new JsonObject() : base.deepCopy();
        JsonObject req = JsonParser.parseString(requestBody).getAsJsonObject();
        for (String k : req.keySet()) rec.add(k, req.get(k));
        rec.addProperty("id", id);
        if (!rec.has("farmName") || rec.get("farmName").isJsonNull()) rec.addProperty("farmName", farmName == null ? "" : farmName);
        return rec;
    }

    private static boolean matches(String listType, JsonObject rec) {
        if (listType == null || listType.isEmpty()) return true;
        return rec.has("type") && !rec.get("type").isJsonNull() && listType.equals(rec.get("type").getAsString());
    }

    private static long idOf(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        return o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsLong() : Long.MIN_VALUE;
    }

    /** New record goes to the top (when it belongs in this filtered list). */
    public static String add(String listJson, String listType, JsonObject rec) {
        JsonArray in = JsonParser.parseString(listJson).getAsJsonArray();
        if (!matches(listType, rec)) return listJson;
        JsonArray out = new JsonArray();
        out.add(rec);
        for (JsonElement e : in) out.add(e);
        return out.toString();
    }

    /** Edited record: replaced in place, moved out of the list if it no longer fits the filter, or added if it newly fits. */
    public static String replace(String listJson, String listType, long id, JsonObject rec) {
        JsonArray in = JsonParser.parseString(listJson).getAsJsonArray();
        JsonArray out = new JsonArray();
        boolean found = false;
        for (JsonElement e : in) {
            if (idOf(e) == id) { found = true; if (matches(listType, rec)) out.add(rec); }
            else out.add(e);
        }
        if (!found && matches(listType, rec)) { JsonArray top = new JsonArray(); top.add(rec); top.addAll(out); out = top; }
        return out.toString();
    }

    public static String remove(String listJson, long id) {
        JsonArray in = JsonParser.parseString(listJson).getAsJsonArray();
        JsonArray out = new JsonArray();
        for (JsonElement e : in) if (idOf(e) != id) out.add(e);
        return out.toString();
    }

    /** The saved record with this id in a list, or null. */
    public static JsonObject find(String listJson, long id) {
        JsonArray in = JsonParser.parseString(listJson).getAsJsonArray();
        for (JsonElement e : in) if (idOf(e) == id) return e.getAsJsonObject();
        return null;
    }
}

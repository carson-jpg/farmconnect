package com.farmconnect.app.data;

import com.farmconnect.app.data.offline.OfflineRules;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Sits in front of every call: saves what was read, serves the saved copy when there is no network, and holds farm-record changes. */
public class OfflineInterceptor implements Interceptor {
    @Override public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();
        long uid = Session.id();
        String path = req.url().encodedPath();
        if (uid <= 0 || !path.startsWith("/api/")) return chain.proceed(req);

        if ("GET".equals(req.method()) && OfflineRules.cacheable(path)) {
            try {
                Response r = chain.proceed(req);
                if (r.isSuccessful() && r.body() != null) Offline.cachePut(uid, req, r.peekBody(2L * 1024 * 1024 + 1).bytes());
                return r;
            } catch (IOException e) {
                Offline.Entry hit = Offline.entryFor(uid, req);
                if (hit == null) throw e;
                Offline.noteServedFromCache(hit.ts);
                return Offline.fromCache(req, hit);
            }
        }

        if (OfflineRules.queueable(req.method(), path)) {
            Long id = OfflineRules.recordId(path);
            if (OfflineRules.isTempId(id)) {
                Long real = Offline.mappedId(uid, id);
                if (real == null) return Offline.queueWrite(req, uid);          // still waiting to be created: change it on the phone
                req = req.newBuilder().url(req.url().newBuilder().encodedPath(OfflineRules.rewriteId(path, id, real)).build()).build();
            }
            try {
                return chain.proceed(req);
            } catch (IOException e) {
                if (!Offline.looksOffline(e)) throw e;
                return Offline.queueWrite(req, uid);
            }
        }
        return chain.proceed(req);
    }
}

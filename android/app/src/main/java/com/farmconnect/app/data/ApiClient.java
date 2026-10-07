package com.farmconnect.app.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Emulator -> your PC: 10.0.2.2. Real phone: use your PC's LAN IP, e.g. http://192.168.1.10:8080/
    private static final String BASE_URL = "http://127.0.0.1:8080/";
    private static ApiService service;

    /** Turns "/api/files/products/1/x.jpg" into a full URL the image loader can fetch. */
    public static String absolute(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String base = BASE_URL.endsWith("/") ? BASE_URL.substring(0, BASE_URL.length() - 1) : BASE_URL;
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    public static synchronized ApiService get() {
        if (service == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BASIC);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request.Builder rb = chain.request().newBuilder();
                        String t = Session.token();
                        if (t != null) rb.header("Authorization", "Bearer " + t);
                        return chain.proceed(rb.build());
                    })
                    .addInterceptor(log)
                    .build();
            service = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
        }
        return service;
    }
}

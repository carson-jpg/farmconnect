package com.farmconnect.app;

import android.app.Application;
import com.farmconnect.app.data.Session;

public class FarmApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Session.init(this);
    }
}

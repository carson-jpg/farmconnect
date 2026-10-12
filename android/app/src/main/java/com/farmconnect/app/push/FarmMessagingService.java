package com.farmconnect.app.push;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Receives Firebase messages, also while the app is closed. */
public class FarmMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) { Push.register(this, token); }

    @Override public void onMessageReceived(RemoteMessage message) { Push.show(this, message.getData()); }
}

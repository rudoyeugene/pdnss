package com.rudyii.pdnss.services;

import static com.rudyii.pdnss.common.Constants.APP_NAME;
import static com.rudyii.pdnss.common.Constants.SERVICE_NOTIFICATION_ID;
import static com.rudyii.pdnss.common.Constants.SERVICE_NOTIFICATION_NAME;
import static com.rudyii.pdnss.common.Utils.updatePdnsSettingsOnNetworkChange;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.rudyii.pdnss.R;

public class NetworkMonitor extends Service {
    private static boolean isRunning = false;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public static boolean isRunning() {
        return isRunning;
    }

    private static void serviceStarted() {
        isRunning = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updatePdnsSettingsOnNetworkChange();
            }
        };
        createServiceNotification();
        serviceStarted();
        Log.i(APP_NAME, "NetworkMonitor created");
    }

    private void createServiceNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, SERVICE_NOTIFICATION_NAME)
                .setContentTitle(getString(R.string.txt_notification_service_title))
                .setContentText(getString(R.string.txt_notification_service_body))
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setSilent(true)
                .build();

        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification);

        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning()) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
            serviceStarted();
            Log.i(APP_NAME, "NetworkMonitor started");
        } else {
            Log.i(APP_NAME, "NetworkMonitor already running");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            Log.i(APP_NAME, "NetworkMonitor stopped");
        } catch (Exception e) {
            Log.w(APP_NAME, "NetworkMonitor was not running");
        }

        isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

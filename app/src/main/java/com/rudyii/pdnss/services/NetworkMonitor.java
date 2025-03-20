package com.rudyii.pdnss.services;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.SERVICE_NOTIFICATION_ID;
import static com.rudyii.pdnss.types.Constants.SERVICE_NOTIFICATION_NAME;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;

public class NetworkMonitor extends Service {
    private boolean isRunning = false;

    public boolean isRunning() {
        return isRunning;
    }

    private void serviceStarted() {
        isRunning = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createServiceNotification();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(SERVICE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning()) {
            getAppContext().getConnectivityManager().registerDefaultNetworkCallback(getAppContext().getNetworkCallback());
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
            getAppContext().getConnectivityManager().unregisterNetworkCallback(getAppContext().getNetworkCallback());
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

    private PrivateDnsSwitcherApplication getAppContext() {
        return (PrivateDnsSwitcherApplication) getApplicationContext();
    }
}

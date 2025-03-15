package com.rudyii.pdnss;

import static com.rudyii.pdnss.common.Constants.APP_NAME;
import static com.rudyii.pdnss.common.Constants.SERVICE_NOTIFICATION_NAME;
import static com.rudyii.pdnss.common.Constants.STATE_NOTIFICATION_NAME;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.rudyii.pdnss.services.NetworkMonitor;
import com.rudyii.pdnss.services.QuickTile;

public class PrivateDnsSwitcherApplication extends Application {
    private static PrivateDnsSwitcherApplication instance;

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        registerServices();
        createNotificationChannels();
    }

    private void registerServices() {
        TileService.requestListeningState(this, new ComponentName(this, QuickTile.class));

        if (!NetworkMonitor.isRunning()) {
            try {
                Intent service = new Intent(getApplicationContext(), NetworkMonitor.class);
                getApplicationContext().startForegroundService(service);
            } catch (Exception e) {
                Log.w(APP_NAME, "NetworkMonitor Service will start later...");
            }
        }
    }

    private void createNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel serviceChannel = new NotificationChannel(SERVICE_NOTIFICATION_NAME,
                getString(R.string.txt_notification_service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationChannel stateChannel = new NotificationChannel(STATE_NOTIFICATION_NAME,
                getString(R.string.txt_notification_state_channel_name),
                NotificationManager.IMPORTANCE_HIGH);

        notificationManager.createNotificationChannel(serviceChannel);
        notificationManager.createNotificationChannel(stateChannel);
    }
}

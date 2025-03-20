package com.rudyii.pdnss;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.SERVICE_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.Constants.STATE_NOTIFICATION_NAME;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.common.NotificationsUtils;
import com.rudyii.pdnss.common.PermissionsUtil;
import com.rudyii.pdnss.common.SettingsUtil;
import com.rudyii.pdnss.services.NetworkMonitor;
import com.rudyii.pdnss.services.QuickTile;

public class PrivateDnsSwitcherApplication extends Application {
    private QuickTile tile;
    private SettingsUtil settingsUtil;
    private PermissionsUtil permissionsUtil;
    private NotificationsUtils notificationsUtils;

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    @Override
    public void onCreate() {
        super.onCreate();

        settingsUtil = new SettingsUtil(this);
        permissionsUtil = new PermissionsUtil(this);
        notificationsUtils = new NotificationsUtils(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                settingsUtil.updatePdnsSettingsOnNetworkChange(network);
            }
        };

        registerServices();
        createNotificationChannels();
    }

    public void refreshQuickTile() {
        if (tile != null) {
            tile.updateTile();
        }
    }

    public void setQuickTile(QuickTile tile) {
        this.tile = tile;
    }

    public ConnectivityManager getConnectivityManager() {
        return connectivityManager;
    }

    public ConnectivityManager.NetworkCallback getNetworkCallback() {
        return networkCallback;
    }

    public SettingsUtil getSettingsUtil() {
        return settingsUtil;
    }

    public PermissionsUtil getPermissionsUtil() {
        return permissionsUtil;
    }

    public NotificationsUtils getNotificationsUtils() {
        return notificationsUtils;
    }

    private void registerServices() {
        TileService.requestListeningState(this, new ComponentName(this, QuickTile.class));

        try {
            Intent service = new Intent(getApplicationContext(), NetworkMonitor.class);
            getApplicationContext().startForegroundService(service);
        } catch (Exception e) {
            Log.w(APP_NAME, "NetworkMonitor Service will start later...");
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

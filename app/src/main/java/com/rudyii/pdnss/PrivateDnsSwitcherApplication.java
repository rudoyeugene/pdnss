package com.rudyii.pdnss;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.STATE_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.Constants.UNSAFE_DNS_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.PdnsModeType.ON;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.rudyii.pdnss.services.QuickTile;
import com.rudyii.pdnss.utils.AppState;
import com.rudyii.pdnss.utils.NotificationsUtils;
import com.rudyii.pdnss.utils.PdnssNetworkCallback;
import com.rudyii.pdnss.utils.PermissionsUtils;
import com.rudyii.pdnss.utils.SecurityScoreUtil;
import com.rudyii.pdnss.utils.SettingsUtils;

public class PrivateDnsSwitcherApplication extends Application {
    private QuickTile tile;
    private SettingsUtils settingsUtils;
    private PermissionsUtils permissionsUtils;
    private NotificationsUtils notificationsUtils;
    private SecurityScoreUtil securityScoreUtil;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private AppState appState;

    @Override
    public void onCreate() {
        super.onCreate();

        securityScoreUtil = new SecurityScoreUtil();
        settingsUtils = new SettingsUtils(this);
        permissionsUtils = new PermissionsUtils(this);
        notificationsUtils = new NotificationsUtils(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        appState = new AppState();
        networkCallback = new PdnssNetworkCallback(this);

        if (ON.equals(settingsUtils.getPDNSState())) {
            securityScoreUtil.enabled();
        }

        registerServices();
        registerNetworkCallback();
        createNotificationChannels();
    }

    public void registerNetworkCallback() {
        if (connectivityManager == null || networkCallback == null) {
            return;
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
            Log.i(APP_NAME, "Default NetworkCallback registered");
        } catch (IllegalArgumentException e) {
            Log.w(APP_NAME, "NetworkCallback already registered, re-registering");
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception e) {
            Log.e(APP_NAME, "Failed to register NetworkCallback", e);
        }
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

    public SettingsUtils getSettingsUtils() {
        return settingsUtils;
    }

    public PermissionsUtils getPermissionsUtils() {
        return permissionsUtils;
    }

    public NotificationsUtils getNotificationsUtils() {
        return notificationsUtils;
    }

    public AppState getAppState() {
        return appState;
    }

    public SecurityScoreUtil getSecurityScoreUtil() {
        return securityScoreUtil;
    }

    private void registerServices() {
        TileService.requestListeningState(this, new ComponentName(this, QuickTile.class));
    }

    private void createNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel stateChannel = new NotificationChannel(STATE_NOTIFICATION_NAME,
                getString(R.string.txt_notification_state_channel_name),
                NotificationManager.IMPORTANCE_HIGH);

        NotificationChannel unsafeDnsChannel = new NotificationChannel(UNSAFE_DNS_NOTIFICATION_NAME,
                getString(R.string.txt_notification_state_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);

        notificationManager.createNotificationChannel(stateChannel);
        notificationManager.createNotificationChannel(unsafeDnsChannel);
    }
}
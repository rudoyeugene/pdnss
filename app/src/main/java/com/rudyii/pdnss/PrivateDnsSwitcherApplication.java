package com.rudyii.pdnss;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.MONITOR_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.PdnsModeType.ON;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.rudyii.pdnss.services.PdnsMonitorService;
import com.rudyii.pdnss.services.QuickTile;
import com.rudyii.pdnss.utils.AppState;
import com.rudyii.pdnss.utils.NotificationsUtils;
import com.rudyii.pdnss.utils.PdnssNetworkCallback;
import com.rudyii.pdnss.utils.PermissionsUtils;
import com.rudyii.pdnss.utils.SecurityScoreUtil;
import com.rudyii.pdnss.utils.SettingsUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class PrivateDnsSwitcherApplication extends Application {
    private final AtomicBoolean networkCallbackRegistered = new AtomicBoolean(false);
    private SettingsUtils settingsUtils;
    private PermissionsUtils permissionsUtils;
    private NotificationsUtils notificationsUtils;
    private SecurityScoreUtil securityScoreUtil;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private AppState appState;
    private volatile QuickTile tile;
    private volatile boolean monitorServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        securityScoreUtil = new SecurityScoreUtil(this);
        settingsUtils = new SettingsUtils(this);
        permissionsUtils = new PermissionsUtils(this);
        notificationsUtils = new NotificationsUtils(this);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        appState = new AppState();
        networkCallback = new PdnssNetworkCallback(this);

        if (ON.equals(settingsUtils.getPDNSState())) {
            securityScoreUtil.enabled();
        } else {
            securityScoreUtil.disabled();
        }

        createNotificationChannels();
        registerServices();
        registerNetworkCallback();
        PdnsMonitorService.start(this);
    }

    public void registerNetworkCallback() {
        if (connectivityManager == null || networkCallback == null) {
            return;
        }
        if (networkCallbackRegistered.compareAndSet(false, true)) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
                Log.i(APP_NAME, "Default NetworkCallback registered");
            } catch (Exception e) {
                networkCallbackRegistered.set(false);
                Log.e(APP_NAME, "Failed to register NetworkCallback", e);
            }
        }
    }

    public boolean isMonitorServiceRunning() {
        return monitorServiceRunning;
    }

    public void setMonitorServiceRunning(boolean running) {
        this.monitorServiceRunning = running;
    }

    public void refreshQuickTile() {
        try {
            TileService.requestListeningState(this, new ComponentName(this, QuickTile.class));
        } catch (Exception e) {
            Log.w(APP_NAME, "Failed to request QuickTile listening state: " + e.getMessage());
        }

        QuickTile currentTile = tile;
        if (currentTile != null) {
            try {
                currentTile.updateTile();
            } catch (Exception e) {
                Log.w(APP_NAME, "Failed to update active QuickTile: " + e.getMessage());
            }
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

        notificationManager.deleteNotificationChannel("STATE_NOTIFICATIONS");
        notificationManager.deleteNotificationChannel("UNSAFE_DNS_NOTIFICATIONS");

        NotificationChannel monitorChannel = new NotificationChannel(
                MONITOR_NOTIFICATION_NAME,
                getString(R.string.txt_notification_state_channel_name),
                NotificationManager.IMPORTANCE_MIN);
        monitorChannel.setShowBadge(false);

        notificationManager.createNotificationChannel(monitorChannel);
    }
}
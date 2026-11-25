package com.rudyii.pdnss;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.STATE_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.Constants.UNSAFE_DNS_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.PdnsModeType.ON;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.rudyii.pdnss.services.QuickTile;
import com.rudyii.pdnss.utils.AppState;
import com.rudyii.pdnss.utils.NotificationsUtils;
import com.rudyii.pdnss.utils.PdnssNetworkCallback;
import com.rudyii.pdnss.utils.PermissionsUtils;
import com.rudyii.pdnss.utils.SecurityScoreUtil;
import com.rudyii.pdnss.utils.SettingsUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class PrivateDnsSwitcherApplication extends Application {
    private QuickTile tile;
    private SettingsUtils settingsUtils;
    private PermissionsUtils permissionsUtils;
    private NotificationsUtils notificationsUtils;
    private SecurityScoreUtil securityScoreUtil;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private AppState appState;
    private static final int NETWORK_PENDING_INTENT_REQUEST_CODE = 1001;
    private final AtomicBoolean networkCallbackRegistered = new AtomicBoolean(false);

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
        }

        registerServices();
        registerNetworkCallback();
        createNotificationChannels();
    }

    public void registerNetworkCallback() {
        if (connectivityManager == null || networkCallback == null) {
            return;
        }
        if (networkCallbackRegistered.compareAndSet(false, true)) {
            try {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
                registerPersistentNetworkPendingIntent();
                Log.i(APP_NAME, "Default and persistent PendingIntent NetworkCallbacks registered");
            } catch (Exception e) {
                networkCallbackRegistered.set(false);
                Log.e(APP_NAME, "Failed to register NetworkCallback", e);
            }
        }
    }

    private void registerPersistentNetworkPendingIntent() {
        Intent intent = new Intent(this, com.rudyii.pdnss.receivers.NetworkChangeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                NETWORK_PENDING_INTENT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build();

        connectivityManager.registerNetworkCallback(request, pendingIntent);
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
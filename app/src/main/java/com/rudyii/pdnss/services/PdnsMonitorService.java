package com.rudyii.pdnss.services;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.MONITOR_NOTIFICATION_ID;
import static com.rudyii.pdnss.types.Constants.MONITOR_NOTIFICATION_NAME;
import static com.rudyii.pdnss.types.Constants.SETTINGS_PRIVATE_DNS_SPECIFIER;
import static com.rudyii.pdnss.types.PdnsModeType.ON;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.activities.ActivityMain;
import com.rudyii.pdnss.types.PdnsModeType;

public class PdnsMonitorService extends Service {
    private static volatile String lastTitle;
    private static volatile String lastBody;
    private static volatile Boolean lastEnabled;

    public static void start(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, PdnsMonitorService.class);
        try {
            ContextCompat.startForegroundService(appContext, intent);
        } catch (Exception e) {
            Log.w(APP_NAME, "Unable to start PdnsMonitorService from current state: " + e.getMessage());
        }
    }

    public static boolean hasCachedNotification() {
        return lastTitle != null && lastBody != null && lastEnabled != null;
    }

    public static void refreshNotification(Context context) {
        if (context == null) {
            return;
        }
        lastTitle = null;
        lastBody = null;
        lastEnabled = null;
        notifyOrStart(context.getApplicationContext());
    }

    public static void refreshNotification(Context context, @NonNull String title, @NonNull String body, boolean enabled) {
        if (context == null) {
            return;
        }
        lastTitle = title;
        lastBody = body;
        lastEnabled = enabled;
        notifyOrStart(context.getApplicationContext());
    }

    private static void notifyOrStart(Context appContext) {
        if (appContext instanceof PrivateDnsSwitcherApplication app && app.isMonitorServiceRunning()) {
            NotificationManager nm = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(MONITOR_NOTIFICATION_ID, buildMonitorNotification(app));
            }
        } else {
            start(appContext);
        }
    }

    private static Notification buildMonitorNotification(Context context) {
        Intent activityIntent = new Intent(context, ActivityMain.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = lastTitle;
        String body = lastBody;
        Boolean enabled = lastEnabled;

        if (title == null || body == null || enabled == null) {
            PdnsModeType mode = PdnsModeType.UNKNOWN;
            String host = "";
            if (context.getApplicationContext() instanceof PrivateDnsSwitcherApplication app) {
                mode = app.getSettingsUtils().getPDNSState();
                host = app.getSettingsUtils().getSettingsValue(SETTINGS_PRIVATE_DNS_SPECIFIER);
            }

            boolean dnsOn = ON.equals(mode);
            enabled = dnsOn;
            String stateLabel = context.getString(dnsOn
                    ? R.string.txt_notification_state_body_enabled
                    : R.string.txt_notification_state_body_disabled);
            title = context.getString(R.string.txt_notification_state_title, stateLabel);

            String modeDisplay = switch (mode) {
                case ON -> context.getString(R.string.txt_dns_state_on);
                case GOOGLE -> context.getString(R.string.txt_dns_state_google);
                case OFF -> context.getString(R.string.txt_dns_state_off);
                default -> mode.name();
            };
            body = dnsOn && !host.isEmpty()
                    ? modeDisplay + " • " + host
                    : modeDisplay;
        }

        return new NotificationCompat.Builder(context, MONITOR_NOTIFICATION_NAME)
                .setSmallIcon(enabled ? R.drawable.enabled : R.drawable.disabled)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        promoteToForeground();
        if (getApplicationContext() instanceof PrivateDnsSwitcherApplication app) {
            app.setMonitorServiceRunning(true);
            app.registerNetworkCallback();
        }
        Log.i(APP_NAME, "PdnsMonitorService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        if (getApplicationContext() instanceof PrivateDnsSwitcherApplication app) {
            app.setMonitorServiceRunning(true);
            app.registerNetworkCallback();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (getApplicationContext() instanceof PrivateDnsSwitcherApplication app) {
            app.setMonitorServiceRunning(false);
        }
        Log.i(APP_NAME, "PdnsMonitorService destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void promoteToForeground() {
        Notification notification = buildMonitorNotification(this);
        startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        );
    }
}
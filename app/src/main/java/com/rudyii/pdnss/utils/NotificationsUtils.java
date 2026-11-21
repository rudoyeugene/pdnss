package com.rudyii.pdnss.utils;

import static com.rudyii.pdnss.types.Constants.STATE_NOTIFICATION_ID;
import static com.rudyii.pdnss.types.Constants.STATE_NOTIFICATION_NAME;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.R;
import com.rudyii.pdnss.activities.ActivityMain;

public class NotificationsUtils {
    private final PrivateDnsSwitcherApplication context;

    public NotificationsUtils(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    public void showWarning(String warningMessage) {
        Toast.makeText(context, warningMessage, Toast.LENGTH_SHORT).show();
    }

    public void showNotification(String title, String body, boolean enabled) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        Intent activityIntent = new Intent(context, ActivityMain.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, STATE_NOTIFICATION_NAME)
                .setSmallIcon(enabled ? R.drawable.enabled : R.drawable.disabled)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setOngoing(false)
                .build();

        notificationManager.notify(STATE_NOTIFICATION_ID, notification);
    }
}
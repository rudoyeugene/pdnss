package com.rudyii.pdnss.receivers;

import static com.rudyii.pdnss.types.Constants.BOOT_COMPLETED;
import static com.rudyii.pdnss.types.Constants.QUICKBOOT_POWERON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rudyii.pdnss.services.NetworkMonitor;

public class OnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BOOT_COMPLETED.equals(action) || QUICKBOOT_POWERON.equals(action)) {
            Intent service = new Intent(context, NetworkMonitor.class);
            context.startForegroundService(service);
        }
    }
}

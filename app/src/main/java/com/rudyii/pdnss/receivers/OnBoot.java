package com.rudyii.pdnss.receivers;

import static com.rudyii.pdnss.common.Constants.BOOT_COMPLETED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rudyii.pdnss.services.NetworkMonitor;

public class OnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (BOOT_COMPLETED.equals(intent.getAction())) {
            if (NetworkMonitor.isStopped()) {
                Intent service = new Intent(context, NetworkMonitor.class);
                context.startForegroundService(service);
            }
        }
    }
}

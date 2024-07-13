package com.rudyii.pdnss.services;

import static com.rudyii.pdnss.PrivateDnsSwitcherApplication.getNetworkChangeBroadcastReceiver;
import static com.rudyii.pdnss.common.Constants.APP_NAME;
import static com.rudyii.pdnss.common.Constants.CONNECTIVITY_CHANGE;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class NetworkMonitor extends Service {
    private static boolean isRunning = false;

    public static boolean isStopped() {
        return !isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        IntentFilter filter = new IntentFilter(CONNECTIVITY_CHANGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                registerReceiver(getNetworkChangeBroadcastReceiver(), filter, RECEIVER_NOT_EXPORTED);
            } catch (Exception e) {
                Log.w(APP_NAME, "etworkChangeBroadcastReceiver already registered");
            }
        } else {
            try {
                registerReceiver(getNetworkChangeBroadcastReceiver(), filter);
            } catch (Exception e) {
                Log.w(APP_NAME, "etworkChangeBroadcastReceiver already registered");
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

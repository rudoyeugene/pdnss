package com.rudyii.pdnss.services;

import static com.rudyii.pdnss.common.Constants.APP_NAME;
import static com.rudyii.pdnss.common.Utils.updatePdnsSettingsOnNetworkChange;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NetworkMonitor extends Service {
    private static boolean isStarted = false;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public static boolean isStopped() {
        return !isStarted;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updatePdnsSettingsOnNetworkChange();
            }
        };
        Log.i(APP_NAME, "NetworkMonitor created");
        isStarted = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
        Log.i(APP_NAME, "NetworkMonitor started");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectivityManager.unregisterNetworkCallback(networkCallback);
        Log.i(APP_NAME, "NetworkMonitor stopped");

        isStarted = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

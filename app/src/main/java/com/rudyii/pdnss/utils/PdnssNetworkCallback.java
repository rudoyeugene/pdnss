package com.rudyii.pdnss.utils;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.SETTINGS_ACTION_DELAY_MS;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;

public class PdnssNetworkCallback extends ConnectivityManager.NetworkCallback {
    private final PrivateDnsSwitcherApplication context;
    private final Handler actionHandler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private boolean isTaskPending = false;

    public PdnssNetworkCallback(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        if (isTaskPending) {
            Log.d(APP_NAME, "Action skip: task is already pending or running");
            return;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock temporaryLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "PDNSS:TriggerLock");
        int delay = context.getSettingsUtils()
                .getSharedPrefs().getInt(SETTINGS_ACTION_DELAY_MS, 0);
        temporaryLock.acquire(delay + 100);
        Log.i(APP_NAME, "Network change detected, trigger scheduled");
        scheduleDnsSwitch(network, temporaryLock, delay);
    }

    private void scheduleDnsSwitch(Network network, PowerManager.WakeLock temporaryLock, int delay) {
        synchronized (lock) {
            isTaskPending = true;
            actionHandler.postDelayed(() -> performDnsAction(network, temporaryLock), delay);
        }
    }

    private void performDnsAction(Network network, PowerManager.WakeLock temporaryLock) {
        Log.i(APP_NAME, "Switching the Private DNS");

        try {
            context.getSettingsUtils().updatePdnsSettingsOnNetworkChange(network);
        } finally {
            synchronized (lock) {
                if (temporaryLock.isHeld()) {
                    temporaryLock.release();
                }
                isTaskPending = false;
            }
        }
    }
}

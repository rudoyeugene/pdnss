package com.rudyii.pdnss.utils;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.SETTINGS_ACTION_DELAY_MS;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;

public class PdnssNetworkCallback extends ConnectivityManager.NetworkCallback {
    private static final long WAKE_LOCK_GRACE_MS = 5000L;
    private final PrivateDnsSwitcherApplication context;
    private final Handler actionHandler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private Runnable pendingTask;
    private PowerManager.WakeLock currentWakeLock;

    public PdnssNetworkCallback(PrivateDnsSwitcherApplication context) {
        this.context = context;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        scheduleDnsEvaluation(network, "onAvailable");
    }

    @Override
    public void onLost(@NonNull Network network) {
        scheduleDnsEvaluation(null, "onLost");
    }

    private void scheduleDnsEvaluation(Network network, String reason) {
        int delay = Math.max(0, context.getSettingsUtils()
                .getSharedPrefs().getInt(SETTINGS_ACTION_DELAY_MS, 0));

        PowerManager.WakeLock newWakeLock;
        Runnable task;

        synchronized (lock) {
            if (pendingTask != null) {
                actionHandler.removeCallbacks(pendingTask);
                pendingTask = null;
            }
            releaseWakeLockLocked();

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            newWakeLock = pm != null
                    ? pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PDNSS:TriggerLock")
                    : null;
            if (newWakeLock != null) {
                newWakeLock.setReferenceCounted(false);
                newWakeLock.acquire(delay + WAKE_LOCK_GRACE_MS);
            }
            currentWakeLock = newWakeLock;

            task = () -> performDnsAction(network, newWakeLock);
            pendingTask = task;
        }

        Log.i(APP_NAME, "Network event (" + reason + "), DNS evaluation scheduled with delay: " + delay + " ms");
        boolean posted = actionHandler.postDelayed(task, delay);
        if (!posted) {
            synchronized (lock) {
                if (pendingTask == task) {
                    pendingTask = null;
                }
                releaseWakeLockLocked();
            }
        }
    }

    private void performDnsAction(Network network, PowerManager.WakeLock wakeLockForRun) {
        Log.i(APP_NAME, "Switching the Private DNS");
        try {
            context.getSettingsUtils().updatePdnsSettingsOnNetworkChange(network);
            context.getAppState().updatePdnsState(SystemClock.elapsedRealtime());
        } finally {
            synchronized (lock) {
                if (currentWakeLock == wakeLockForRun) {
                    releaseWakeLockLocked();
                } else if (wakeLockForRun != null && wakeLockForRun.isHeld()) {
                    wakeLockForRun.release();
                }
                pendingTask = null;
            }
        }
    }

    private void releaseWakeLockLocked() {
        if (currentWakeLock != null && currentWakeLock.isHeld()) {
            currentWakeLock.release();
        }
        currentWakeLock = null;
    }
}
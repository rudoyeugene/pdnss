package com.rudyii.pdnss.utils;

import static com.rudyii.pdnss.types.Constants.APP_NAME;
import static com.rudyii.pdnss.types.Constants.DEFAULT_ACTION_DELAY_SEC;
import static com.rudyii.pdnss.types.Constants.MAX_ACTION_DELAY_SEC;
import static com.rudyii.pdnss.types.Constants.MIN_ACTION_DELAY_SEC;
import static com.rudyii.pdnss.types.Constants.SETTINGS_ACTION_DELAY_SEC;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PdnssNetworkCallback extends ConnectivityManager.NetworkCallback {
    private final PrivateDnsSwitcherApplication app;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "pdnss-dns-eval");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean evaluationInProgress = new AtomicBoolean(false);
    private volatile int lastStateHash = 0;

    public PdnssNetworkCallback(PrivateDnsSwitcherApplication app) {
        super(FLAG_INCLUDE_LOCATION_INFO);
        this.app = app;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        ConnectivityManager cm = app.getConnectivityManager();
        NetworkCapabilities caps = cm != null ? cm.getNetworkCapabilities(network) : null;
        if (caps != null) {
            extractAndCacheWifiSsid(caps);
            int stateHash = computeStateHash(caps, app.getSettingsUtils().getCachedWifiSsid());
            if (stateHash == lastStateHash) {
                return;
            }
            lastStateHash = stateHash;
        }
        scheduleDnsEvaluation();
    }

    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        extractAndCacheWifiSsid(networkCapabilities);
        int stateHash = computeStateHash(networkCapabilities, app.getSettingsUtils().getCachedWifiSsid());
        if (stateHash == lastStateHash) {
            return;
        }
        lastStateHash = stateHash;
        scheduleDnsEvaluation();
    }

    @Override
    public void onLost(@NonNull Network network) {
        lastStateHash = 0;
        app.getSettingsUtils().clearCachedWifiInfo();
        scheduleDnsEvaluation();
    }

    public void scheduleDnsEvaluation() {
        if (!evaluationInProgress.compareAndSet(false, true)) {
            Log.d(APP_NAME, "DNS evaluation already scheduled or in progress; ignoring duplicate trigger");
            return;
        }

        int delaySeconds = Math.clamp(
                app.getSettingsUtils().getSharedPrefs()
                        .getInt(SETTINGS_ACTION_DELAY_SEC, DEFAULT_ACTION_DELAY_SEC),
                MIN_ACTION_DELAY_SEC,
                MAX_ACTION_DELAY_SEC
        );
        long delayMs = delaySeconds * 1000L;

        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (delayMs > 0 && pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, APP_NAME + ":DnsEvalDelay");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(delayMs + 5000L);
        }

        final PowerManager.WakeLock activeWakeLock = wakeLock;
        Log.i(APP_NAME, "Scheduling DNS evaluation in " + delaySeconds + " s");
        try {
            scheduler.schedule(() -> {
                try {
                    app.getSettingsUtils().updatePdnsSettingsOnNetworkChange(null);
                } catch (Exception e) {
                    Log.e(APP_NAME, "Error during scheduled DNS evaluation", e);
                } finally {
                    if (activeWakeLock != null && activeWakeLock.isHeld()) {
                        activeWakeLock.release();
                    }
                    evaluationInProgress.set(false);
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            if (activeWakeLock != null && activeWakeLock.isHeld()) {
                activeWakeLock.release();
            }
            evaluationInProgress.set(false);
            Log.e(APP_NAME, "Failed to schedule DNS evaluation", e);
        }
    }

    private void extractAndCacheWifiSsid(@NonNull NetworkCapabilities caps) {
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                && caps.getTransportInfo() instanceof WifiInfo wifiInfo) {
            app.getSettingsUtils().setCachedWifiSsid(wifiInfo.getSSID());
        }
    }

    private int computeStateHash(@NonNull NetworkCapabilities caps, String cachedSsid) {
        boolean isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        boolean isWiFi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        return Objects.hash(isVpn, isWiFi, isCellular, isWiFi ? cachedSsid : null);
    }
}
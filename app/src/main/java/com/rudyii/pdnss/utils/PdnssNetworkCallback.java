package com.rudyii.pdnss.utils;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import com.rudyii.pdnss.PrivateDnsSwitcherApplication;
import com.rudyii.pdnss.types.NetworkSnapshot;

import java.util.Objects;

public class PdnssNetworkCallback extends ConnectivityManager.NetworkCallback {
    private final PrivateDnsSwitcherApplication context;
    private int lastSignature = -1;

    public PdnssNetworkCallback(PrivateDnsSwitcherApplication context) {
        super(FLAG_INCLUDE_LOCATION_INFO);
        this.context = context;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        lastSignature = -1;
        NetworkCapabilities caps = context.getConnectivityManager().getNetworkCapabilities(network);
        NetworkSnapshot snapshot = caps != null
                ? NetworkSnapshot.fromCapabilities(network, caps, context.getSettingsUtils().getCachedWifiSsid())
                : null;
        if (snapshot != null && snapshot.wifiSsid() != null) {
            context.getSettingsUtils().setCachedWifiSsid(snapshot.wifiSsid());
        }
        PdnsPolicyScheduler.scheduleEvaluation(context, "onAvailable", snapshot, null);
    }

    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
        NetworkSnapshot snapshot = NetworkSnapshot.fromCapabilities(
                network,
                capabilities,
                context.getSettingsUtils().getCachedWifiSsid()
        );
        if (snapshot.wifiSsid() != null) {
            context.getSettingsUtils().setCachedWifiSsid(snapshot.wifiSsid());
        }

        int signature = Objects.hash(
                snapshot.isVpn(),
                snapshot.isWiFi(),
                snapshot.isCellular(),
                snapshot.wifiSsid()
        );
        if (signature != lastSignature) {
            lastSignature = signature;
            PdnsPolicyScheduler.scheduleEvaluation(context, "onCapabilitiesChanged", snapshot, null);
        }
    }

    @Override
    public void onLost(@NonNull Network network) {
        lastSignature = -1;
        context.getSettingsUtils().clearCachedWifiInfo();
        PdnsPolicyScheduler.scheduleEvaluation(context, "onLost", null, null);
    }
}
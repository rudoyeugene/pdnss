package com.rudyii.pdnss.types;

import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public record NetworkSnapshot(
        @Nullable Network network,
        boolean isVpn,
        boolean isWiFi,
        boolean isCellular,
        @Nullable String wifiSsid
) {
    public static NetworkSnapshot fromCapabilities(
            @Nullable Network network,
            @NonNull NetworkCapabilities capabilities,
            @Nullable String fallbackSsid) {
        boolean isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        boolean isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        String ssid = fallbackSsid;
        TransportInfo transportInfo = capabilities.getTransportInfo();
        if (transportInfo instanceof WifiInfo wifiInfo) {
            String rawSsid = wifiInfo.getSSID();
            if (rawSsid != null && !"<unknown ssid>".equals(rawSsid)) {
                ssid = rawSsid.replace("\"", "");
            }
        }

        return new NetworkSnapshot(network, isVpn, isWiFi, isCellular, ssid);
    }
}
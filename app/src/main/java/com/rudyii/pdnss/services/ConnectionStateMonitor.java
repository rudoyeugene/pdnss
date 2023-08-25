package com.rudyii.pdnss.services;

import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_OFF;
import static android.app.admin.DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.rudyii.pdnss.Application.getContext;
import static com.rudyii.pdnss.common.Constants.SETTINGS_PRIVATE_DNS_MODE;
import static com.rudyii.pdnss.common.Constants.VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING;
import static com.rudyii.pdnss.common.Utils.getSettingsValue;
import static com.rudyii.pdnss.common.Utils.updatePdnsModeSettings;
import static com.rudyii.pdnss.services.QuickTileService.refreshQsTile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import com.rudyii.pdnss.R;

public class ConnectionStateMonitor extends BroadcastReceiver {
    final NetworkRequest networkRequest;

    public ConnectionStateMonitor() {
        networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                SharedPreferences sharedPref = getContext().getSharedPreferences(
                        getContext().getString(R.string.settings_name), Context.MODE_PRIVATE);
                boolean wasActive = sharedPref.getBoolean(getContext().getString(R.string.settings_name_last_pdns_state), false);

                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    boolean pDnsStateOn = VALUE_PRIVATE_DNS_MODE_PROVIDER_HOSTNAME_STRING.equals(getSettingsValue(SETTINGS_PRIVATE_DNS_MODE));
                    boolean disableWhileVnp = sharedPref.getBoolean(getContext().getString(R.string.settings_name_disable_while_vpn), false);
                    if (disableWhileVnp && pDnsStateOn) {
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_OFF);
                        refreshQsTile();
                    }
                } else {
                    if (wasActive) {
                        updatePdnsModeSettings(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
                        refreshQsTile();
                    }
                }
            }
        }

    }
}
